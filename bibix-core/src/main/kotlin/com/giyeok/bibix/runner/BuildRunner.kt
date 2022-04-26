package com.giyeok.bibix.runner

import com.giyeok.bibix.base.*
import com.giyeok.bibix.buildscript.*
import com.giyeok.bibix.plugins.BibixPlugin
import com.giyeok.bibix.runner.Constants.BIBIX_VERSION
import com.giyeok.bibix.utils.*
import kotlinx.coroutines.*
import org.codehaus.plexus.classworlds.ClassWorld
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BuildRunner(
  val buildGraph: BuildGraph,
  val rootScript: BibixPlugin,
  val bibixPlugins: Map<String, BibixPlugin>,
  val repo: Repo,
  val routineManager: RoutineManager,
  val progressIndicatorContainer: ProgressIndicatorContainer,
  val bibixArgs: Map<CName, BibixValue> = mapOf(),
  val actionArgs: ListValue? = null,
  val classWorld: ClassWorld = ClassWorld(),
  val routineLogger: BuildTaskRoutineLogger = BuildTaskRoutineLoggerImpl(),
) {
  private val importSourceResolver = ImportSourceResolver(repo)
  private val taskObjectIds = mutableMapOf<BuildTask, BibixIdProto.ObjectId>()
  private val coercer = Coercer(buildGraph, this)

  private val realmIdCounter = AtomicInteger()
  fun nextRealmId(): String = "cw${realmIdCounter.incrementAndGet()}"

  private fun getProgressIndicator(): ProgressIndicator =
    progressIndicatorContainer.ofCurrentThread()

  private fun addSourceId(sourceId: BibixIdProto.SourceId): SourceId = when (sourceId.sourceCase) {
    BibixIdProto.SourceId.SourceCase.ROOT_SOURCE -> BibixRootSourceId
    BibixIdProto.SourceId.SourceCase.MAIN_SOURCE -> MainSourceId
    BibixIdProto.SourceId.SourceCase.BIBIX_INTERNAL_SOURCE -> BibixInternalSourceId(sourceId.bibixInternalSource)
    BibixIdProto.SourceId.SourceCase.LOCAL_SOURCE -> LocalSourceId(sourceId.localSource)
    BibixIdProto.SourceId.SourceCase.REMOTE_SOURCE -> {
      val remoteSourceId = buildGraph.addRemoteSource(sourceId.remoteSource)
      RemoteSourceId(remoteSourceId)
    }
    else -> TODO()
  }

  fun runTargets(buildRequestName: String, targets: Collection<CName>) {
    val rootTask = BuildTask.BuildRequest(buildRequestName)
    val coroutineContext = routineManager.coroutineDispatcher + BuildTaskElement(rootTask)
    CoroutineScope(coroutineContext).launch(coroutineContext) {
      runTasks(rootTask, targets.map { BuildTask.ResolveName(it) })
      routineManager.buildFinished(buildRequestName)
    }
  }

  private suspend fun runTasks(requestTask: BuildTask, tasks: List<BuildTask>) =
    coroutineScope {
      tasks.map { task ->
        async(currentCoroutineContext() + BuildTaskElement(task)) {
          runTask(requestTask, task)
        }
      }
    }.awaitAll()

  suspend fun runTask(requestTask: BuildTask?, task: BuildTask): Any {
    // requestTask가 실행되려면 task의 결과가 필요하다는 의미.
    // TODO 싸이클이 생기면 오류 발생하고 종료
    return coroutineScope {
      routineManager.asyncInvoke(task) {
        when (task) {
          is BuildTask.BuildRequest -> {
            // do nothing? 애당초 여기에 올 수가 있나?
          }
          is BuildTask.ResolveName -> resolveName(task)
          is BuildTask.CallAction -> callAction(task)
          is BuildTask.EvalExpr -> evalExpr(task)
          is BuildTask.ResolveImport -> resolveImport(task)
          is BuildTask.ResolveImportSource -> resolveImportSource(task)
        }
      }
      routineManager.waitForTaskResult(task)
    }
  }

  private suspend inline fun resolveName(task: BuildTask.ResolveName): Any {
    val cname = task.cname
    val value = buildGraph.names[cname]
    return if (value == null) {
      // parent로 찾기
      var parent = cname.parent()
      while (!buildGraph.names.containsKey(parent)) {
        if (parent.tokens.isEmpty()) {
          throw BibixBuildException(task, "Cannot find name $cname")
        }
        parent = parent.parent()
      }
      val parentValue = buildGraph.names.getValue(parent)
      if (parentValue is CNameValue.DeferredImport) {
        val importedSourceId =
          runTask(task, BuildTask.ResolveImport(task.cname.sourceId, parentValue.deferredImportId))
        // import가 정상적으로 완료됐으면 import된 스크립트의 모든 element들은 buildGraph에 들어가 있어야 함
        when (importedSourceId) {
          is SourceId -> {
            synchronized(this) {
              buildGraph.replaceValue(parent, CNameValue.LoadedImport(importedSourceId))
            }
            runTask(task, BuildTask.ResolveName(CName(importedSourceId, task.cname.tokens.drop(1))))
          }
          is CNameValue.NamespaceValue -> {
            runTask(
              task,
              BuildTask.ResolveName(importedSourceId.cname.append(task.cname.tokens.drop(1)))
            )
          }
          // import all일 때는 SourceId, import from일 때는 namespace value.
          // import from일 때 namespace 외에 다른 거일 수도 있나?
          else -> TODO()
        }
      } else {
        parentValue as CNameValue.LoadedImport
        runTask(task, BuildTask.ResolveName(CName(parentValue.sourceId, task.cname.tokens.drop(1))))
      }
    } else {
      when (value) {
        is CNameValue.LoadedImport -> value
        is CNameValue.DeferredImport ->
          runTask(task, BuildTask.ResolveImport(task.cname.sourceId, value.deferredImportId))
        is CNameValue.ExprValue -> {
          val evalTask = BuildTask.EvalExpr(
            task.cname.sourceId,
            value.exprGraphId,
            buildGraph.exprGraphs[value.exprGraphId].mainNode,
            null
          )
          val evalResult = runTask(task, evalTask)
          buildGraph.replaceValue(task.cname, CNameValue.EvaluatedValue(evalResult as BibixValue))
          // CallExpr 실행시 evalTask -> target id 저장해두기
          synchronized(this) {
            taskObjectIds[evalTask]?.let { objectId ->
              taskObjectIds[task] = objectId
              // main source에서 정의한 이름이면 링크 만들기
              if (task.cname.sourceId == MainSourceId) {
                repo.linkNameTo(task.cname.tokens.joinToString("."), objectId)
              }
            }
          }
          evalResult
        }
        is CNameValue.EvaluatedValue -> value.value
        is CNameValue.NamespaceValue -> value
        is CNameValue.ClassType -> value
        is CNameValue.EnumType -> value
        is CNameValue.ArgVar -> {
          // 사용자가 지정한 arg가 있으면 그 값을 반환하고
          // 없으면 default value를 evaluate
          val userArg = bibixArgs[task.cname]
          if (userArg != null) {
            return userArg
          } else {
            if (value.defaultValueId == null) {
              throw BibixBuildException(task, "argument not specified: ${task.cname}")
            }
            val exprGraph = buildGraph.exprGraphs[value.defaultValueId]
            return runTask(
              task,
              BuildTask.EvalExpr(
                task.cname.sourceId,
                value.defaultValueId,
                exprGraph.mainNode,
                null
              )
            )
          }
        }
        is CNameValue.BuildRuleValue -> {
          // value.implName.sourceId is BibixRootSourceId 인 경우도 처리
          when {
            value.implName.sourceId == BibixRootSourceId -> {
              check(value.implName.tokens == listOf("$$"))
              BuildRuleImplInfo(
                value,
                value.implName.sourceId,
                objectIdHash {
                  this.rootSource = "$BIBIX_VERSION:root:${value.className}"
                },
                rootScript.classes.getClass(value.className),
                value.methodName,
                value.params,
                value.returnType,
              )
            }
            value.implName.sourceId is BibixInternalSourceId &&
              value.implName.tokens == listOf("$$") -> {
              // native impls
              val plugin = bibixPlugins.getValue(value.implName.sourceId.name)
              BuildRuleImplInfo(
                value,
                value.implName.sourceId,
                objectIdHash {
                  this.bibixInternalSource =
                    "$BIBIX_VERSION:internal ${value.implName.sourceId.name}:${value.className}"
                },
                plugin.classes.getClass(value.className),
                value.methodName,
                value.params,
                value.returnType,
              )
            }
            else -> {
              val implResolveTask = BuildTask.ResolveName(value.implName)
              val implResult0 = runTask(task, implResolveTask)
              val implResult = coercer.coerce(
                task, task.cname.sourceId, implResult0 as BibixValue,
                // TODO resolveClassPkgs 호출하도록 수정
                CustomType(CName(BibixInternalSourceId("jvm"), "ClassPaths")),
                null
              )
              implResult as ClassInstanceValue
              val ruleImplInfo = synchronized(this) {
                val implObjectId = taskObjectIds.getValue(implResolveTask)
                val implObjectIdHash = implObjectId.hashString()
                val implObjectIdHashHex = implObjectIdHash.toHexString()
                // implResult의 objectId가 같으면 realm 재사용
                val realm = try {
                  classWorld.getRealm(implObjectIdHashHex)
                } catch (e: NoSuchRealmException) {
                  val newRealm = classWorld.newRealm(implObjectIdHashHex)
                  val cps =
                    ((implResult.value) as SetValue).values.map { (it as PathValue).path }
                  cps.forEach {
                    newRealm.addURL(it.canonicalFile.toURI().toURL())
                  }
                  newRealm
                }

                BuildRuleImplInfo(
                  value,
                  value.implName.sourceId,
                  objectIdHash { this.objectIdHashString = implObjectIdHash },
                  realm.loadClass(value.className),
                  value.methodName,
                  value.params,
                  value.returnType
                )
              }
              // TODO buildGraph.replaceValue(task.cname, implResult)
              ruleImplInfo
            }
          }
        }
        is CNameValue.ActionRuleValue -> {
          when {
            value.implName.sourceId is BibixInternalSourceId &&
              value.implName.tokens == listOf("$$") -> {
              val plugin = bibixPlugins.getValue(value.implName.sourceId.name)
              ActionRuleImplInfo(
                value,
                value.implName.sourceId,
                plugin.classes.getClass(value.className),
                value.methodName,
                value.params
              )
            }
            else -> {
              val implResult0 = runTask(task, BuildTask.ResolveName(value.implName))
              val implResult = coercer.coerce(
                task,
                task.cname.sourceId,
                implResult0 as BibixValue,
                CustomType(CName(BibixInternalSourceId("jvm"), "ClassPaths")),
                null
              )
              implResult as ClassInstanceValue
              val cps =
                ((implResult.value) as SetValue).values.map { (it as PathValue).path }
              val realm = synchronized(this) {
                val realm = classWorld.newRealm(nextRealmId())
                cps.forEach {
                  realm.addURL(it.canonicalFile.toURI().toURL())
                }
                realm
              }
              val actionImplInfo = ActionRuleImplInfo(
                value,
                value.implName.sourceId,
                realm.loadClass(value.className),
                value.methodName,
                value.params,
              )
              // TODO buildGraph.replaceValue(task.cname, implResult)
              actionImplInfo
            }
          }
        }
        is CNameValue.ActionCallValue ->
          runTask(task, BuildTask.CallAction(task.cname.sourceId, value.exprGraphId))
      }
    }
  }

  private suspend fun callAction(task: BuildTask.CallAction): Any {
    val exprGraph = buildGraph.exprGraphs[task.exprGraphId]
    val callExpr = exprGraph.callExprs[(exprGraph.mainNode as ExprNode.CallExprNode).callExprId]
    val targetTask = BuildTask.EvalExpr(task.origin, task.exprGraphId, callExpr.target, null)
    val namedParams = callExpr.params.namedParams.entries.toList().sortedBy { it.key }
    val paramTasks = (callExpr.params.posParams + namedParams.map { it.value }).map {
      BuildTask.EvalExpr(
        task.origin,
        task.exprGraphId,
        it,
        null
      )
    }
    val ruleImplInfoAsync = coroutineScope {
      async(currentCoroutineContext()) {
        runTask(task, targetTask) as ActionRuleImplInfo
      }
    }
    val paramValues = runTasks(task, paramTasks).map { it as BibixValue }
    val ruleImplInfo = ruleImplInfoAsync.await()

    val instance = ruleImplInfo.cls.getDeclaredConstructor().newInstance()
    val methodName = ruleImplInfo.methodName ?: "run"
    val method = ruleImplInfo.cls.getMethod(methodName, ActionContext::class.java)

    val posParams = paramValues.take(callExpr.params.posParams.size)
    val namedParamsMap =
      namedParams.map { it.key }.zip(paramValues.drop(callExpr.params.posParams.size)).toMap()
    val args = organizeParams(task, task.origin, ruleImplInfo.params, posParams, namedParamsMap)

    val progressIndicator = getProgressIndicator()

    val actionContext = ActionContext(ruleImplInfo.origin, args, progressIndicator)

    val callingTarget = "${ruleImplInfo.cls.canonicalName}:$methodName"

    progressIndicator.updateProgressDescription("Calling $callingTarget...")
    val invokeResult = try {
      method.invoke(instance, actionContext)
    } catch (e: Exception) {
      throw BibixBuildException(task, "action failed", e)
    }
    progressIndicator.updateProgressDescription("Continuing from $callingTarget...")

    return suspendCoroutine { cont ->
      fun doNext(result: Any?) {
        when (result) {
          null -> cont.resume(NoneValue)
          is BuildRuleReturn.ValueReturn ->
            throw BibixBuildException(task, "action rule must not return a value")
          is BuildRuleReturn.FailedReturn ->
            throw BibixBuildException(task, result.exception.message ?: "", result.exception)
          is BuildRuleReturn.DoneReturn -> cont.resume(NoneValue)
          is BuildRuleReturn.EvalAndThen -> {
            // `result.ruleName`을 . 단위로 끊어서 rule 이름을 찾은 다음
            val nameTokens = result.ruleName.split('.')
            val callingName = CName(ruleImplInfo.origin, nameTokens)
            // 해당 rule에 `result.params`을 줘서 실행하고
            routineManager.executeSuspend(task, {
              val calling = runTask(task, BuildTask.ResolveName(callingName)) as BuildRuleImplInfo
              callBuildRule(
                task,
                calling.origin,
                calling,
                listOf(),
                result.params,
              )
            }) { buildResult ->
              // 결과를 `result.whenDone`으로 전달해서 반복
              progressIndicator.updateProgressDescription("Calling (again) $callingTarget...")
              val nextResult = try {
                result.whenDone(buildResult)
              } catch (e: Exception) {
                throw BibixBuildException(task, e.message ?: "", e)
              }
              doNext(nextResult)
              progressIndicator.updateProgressDescription("Continuing from $callingTarget...")
            }
          }
        }
      }
      doNext(invokeResult)
    }
  }

  private fun accessMember(task: BuildTask, value: BibixValue, name: String): BibixValue =
    when (value) {
      is NamedTupleValue -> value.getValue(name)
      is ClassInstanceValue -> accessMember(task, value.value, name)
      else -> throw BibixBuildException(task, "Cannot access $value $name")
    }

  private suspend inline fun evalExpr(task: BuildTask.EvalExpr): Any {
    val exprGraph = buildGraph.exprGraphs[task.exprGraphId]
    return when (val exprNode = task.exprNode) {
      is ExprNode.CallExprNode -> {
        val callExpr = exprGraph.callExprs[exprNode.callExprId]
        val implTask =
          BuildTask.EvalExpr(task.origin, task.exprGraphId, callExpr.target, task.thisValue)
        val ruleImplInfo = runTask(task, implTask) as BuildRuleImplInfo
        val namedParams = callExpr.params.namedParams.entries.toList().sortedBy { it.key }
        val paramTasks = (callExpr.params.posParams + namedParams.map { it.value }).map {
          BuildTask.EvalExpr(
            task.origin,
            task.exprGraphId,
            it,
            task.thisValue
          )
        }
        val params = runTasks(task, paramTasks)

        val paramValues = coercer.toBibixValues(task, params)
        val posParams = paramValues.take(callExpr.params.posParams.size)
        val namedParamsMap =
          namedParams.map { it.key }.zip(paramValues.drop(callExpr.params.posParams.size))
            .toMap()
        callBuildRule(task, task.origin, ruleImplInfo, posParams, namedParamsMap)
      }
      is ExprNode.NameNode -> runTask(task, BuildTask.ResolveName(exprNode.name))
      is ExprNode.MergeOpNode -> {
        coroutineScope {
          val lhsAsync = async {
            runTask(
              task,
              BuildTask.EvalExpr(task.origin, task.exprGraphId, exprNode.lhs, task.thisValue)
            )
          }
          val rhsAsync = async {
            runTask(
              task,
              BuildTask.EvalExpr(task.origin, task.exprGraphId, exprNode.rhs, task.thisValue)
            )
          }
          val listType = ListType(AnyType)
          val lhs =
            coercer.coerce(task, task.origin, lhsAsync.await() as BibixValue, listType, null)
          val rhs =
            coercer.coerce(task, task.origin, rhsAsync.await() as BibixValue, listType, null)
          ListValue((checkNotNull(lhs) as ListValue).values + (checkNotNull(rhs) as ListValue).values)
        }
      }
      is ExprNode.MemberAccessNode -> {
        val result = runTask(
          task,
          BuildTask.EvalExpr(task.origin, task.exprGraphId, exprNode.target, task.thisValue)
        )
        when (result) {
          is CNameValue.EnumType -> {
            check(result.values.contains(exprNode.name))
            EnumValue(result.cname, exprNode.name)
          }
          is CNameValue.NamespaceValue -> {
            check(result.names.contains(exprNode.name))
            runTask(task, BuildTask.ResolveName(result.cname.append(exprNode.name)))
          }
          is BibixValue -> accessMember(task, result, exprNode.name)
          is SourceId -> runTask(task, BuildTask.ResolveName(CName(result, exprNode.name)))
          else -> throw BibixBuildException(task, "Invalid access: $exprNode")
        }
      }
      is ExprNode.ListNode -> {
        val elemResults = runTasks(task, exprNode.elems.map {
          BuildTask.EvalExpr(task.origin, task.exprGraphId, it, task.thisValue)
        })
        val elemResultValues = coercer.toBibixValues(task, elemResults)
        ListValue(elemResultValues)
      }
      is ExprNode.TupleNode -> {
        val elemResults = runTasks(task, exprNode.elems.map {
          BuildTask.EvalExpr(task.origin, task.exprGraphId, it, task.thisValue)
        })
        val elemResultValues = coercer.toBibixValues(task, elemResults)
        TupleValue(elemResultValues)
      }
      is ExprNode.NamedTupleNode -> {
        val elemResults = runTasks(task, exprNode.elems.map {
          BuildTask.EvalExpr(task.origin, task.exprGraphId, it.second, task.thisValue)
        })
        val elemResultValues = coercer.toBibixValues(task, elemResults)
        NamedTupleValue(
          exprNode.elems.zip(elemResultValues).map { it.first.first to it.second })
      }
      is ExprNode.StringLiteralNode -> {
        val exprChunkResults = runTasks(task,
          exprNode.stringElems.filterIsInstance<ExprChunk>().map {
            BuildTask.EvalExpr(task.origin, task.exprGraphId, it.expr, task.thisValue)
          })
        val resultValues = coercer.toBibixValues(task, exprChunkResults)
        val stringBuilder = StringBuilder()
        var i = 0
        exprNode.stringElems.forEach { elem ->
          when (elem) {
            is ExprChunk -> {
              stringBuilder.append(resultValues[i].stringify())
              i += 1
            }
            is StringChunk -> stringBuilder.append(elem.value)
          }
        }
        StringValue(stringBuilder.toString())
      }
      is ExprNode.BooleanLiteralNode -> BooleanValue(exprNode.value)
      is ExprNode.ClassThisRef -> checkNotNull(task.thisValue)
      ExprNode.ActionArgsRef -> actionArgs!!
    }
  }

  private suspend inline fun resolveImport(task: BuildTask.ResolveImport): Any =
    when (val importDef = buildGraph.deferredImports[task.importDefId]) {
      is DeferredImportDef.ImportAll -> {
        val importedSource =
          runTask(task, BuildTask.ResolveImportSource(task.origin, importDef.importSource))
        importedSource as ImportedSource
        synchronized(this) {
          val sourceId = addSourceId(importedSource.sourceId)
          // TODO add to buildGraph
          buildGraph.addDefs(
            sourceId,
            importedSource.buildScript.defs().toKtList(),
            importedSource.baseDirectory,
          )
          sourceId
        }
      }
      is DeferredImportDef.ImportDefaultPlugin -> {
        check(importDef.nameTokens.size == 1)
        val pluginName = importDef.nameTokens.first()
        val bibixPlugin = bibixPlugins[pluginName]
        checkNotNull(bibixPlugin) { "Unknown plugin: $pluginName" }
        val sourceId = BibixInternalSourceId(pluginName)
        synchronized(this) {
          val defs = bibixPlugin.defs
          buildGraph.addDefs(
            sourceId,
            defs,
            NameLookupContext(CName(sourceId), defs).withNative(),
            File(".") // default plugin은 base directory가 없음
          )
          sourceId
        }
      }
      is DeferredImportDef.ImportFrom -> {
        val importedSource =
          runTask(task, BuildTask.ResolveImportSource(task.origin, importDef.importSource))
        importedSource as ImportedSource
        val sourceId = synchronized(this) {
          val sourceId = addSourceId(importedSource.sourceId)
          buildGraph.addDefs(
            sourceId,
            importedSource.buildScript.defs().toKtList(),
            importedSource.baseDirectory
          )
          // println(importedSource)
          sourceId
        }
        runTask(task, BuildTask.ResolveName(CName(sourceId, importDef.nameTokens)))
      }
      is DeferredImportDef.ImportMainSub -> {
        runTask(task, BuildTask.ResolveName(CName(MainSourceId, importDef.nameTokens)))
      }
    }

  private suspend inline fun resolveImportSource(task: BuildTask.ResolveImportSource): Any =
    when (val source = task.importSource) {
      is ImportSource.ImportSourceCall -> {
        val exprGraph = buildGraph.exprGraphs[source.sourceExprId]
        val result = runTask(
          task,
          BuildTask.EvalExpr(source.origin, source.sourceExprId, exprGraph.mainNode, null)
        ) as ClassInstanceValue

        // git과 같은 "root" source에 있는 함수들은 named tuple로 소스에 대한 정보를 반환하고 여기서는 그 값을 받아서 사용
        importSourceResolver.resolveImportSourceCall(result, getProgressIndicator())
      }
      is ImportSource.ImportSourceString -> {
        val exprGraph = buildGraph.exprGraphs[source.stringLiteralExprId]
        val result = runTask(
          task, BuildTask.EvalExpr(
            source.origin, source.stringLiteralExprId, exprGraph.mainNode, null
          )
        )
        val importDirectory =
          coercer.coerce(task, task.origin, result as StringValue, DirectoryType, null)
        importSourceResolver.resolveImportSourcePath(
          repo.mainDirectory,
          importDirectory as DirectoryValue,
          getProgressIndicator()
        )
      }
    }

  private suspend fun organizeParams(
    task: BuildTask,
    origin: SourceId,
    paramDefs: List<Param>,
    posParams: List<BibixValue>,
    namedParams: Map<String, BibixValue>
  ): Map<String, BibixValue> {
    val posParamsZipped = paramDefs.zip(posParams)
    val remainingParams = paramDefs.drop(posParams.size)
    val invalidParams = namedParams.keys - remainingParams.map { it.name }.toSet()
    if (invalidParams.isNotEmpty()) {
      throw BibixBuildException(task, "Invalid parameter name: $invalidParams")
    }

    val paramDefsMap = paramDefs.associateBy { it.name }
    val posParamsMap = posParamsZipped.associate { it.first.name to it.second }

    val unspecifiedParams = paramDefsMap.keys - posParamsMap.keys - namedParams.keys

    val missingParams = unspecifiedParams.filter {
      val paramDef = paramDefsMap.getValue(it)
      !paramDef.optional && paramDef.defaultValueId == null
    }
    if (missingParams.isNotEmpty()) {
      throw BibixBuildException(task, "Missing parameters: $missingParams")
    }

    val defaults = unspecifiedParams.mapNotNull {
      val paramDef = paramDefsMap.getValue(it)
      if (paramDef.defaultValueId == null) null else paramDef.name to paramDef.defaultValueId
    }

    val defaultValues = defaults.map { default ->
      val exprGraph = buildGraph.exprGraphs[default.second]
      val defaultValue =
        runTask(task, BuildTask.EvalExpr(origin, default.second, exprGraph.mainNode, null))
      default.first to (defaultValue as BibixValue)
    }

    val params = posParamsMap + namedParams + defaultValues
    val coercedParams = params.mapValues { (name, value) ->
      coercer.coerce(task, origin, value, paramDefsMap.getValue(name).type, null)
    }

    if (coercedParams.values.contains(null)) {
      val coerceFailedParams = coercedParams.filter { it.value == null }
      throw BibixBuildException(task, "Cannot coerce parameter(s): ${coerceFailedParams.keys}")
    }

    return coercedParams.mapValues { it.value!! }
  }

  private suspend fun callBuildRule(
    task: BuildTask,
    origin: SourceId,
    ruleImplInfo: BuildRuleImplInfo,
    posParams: List<BibixValue>,
    namedParamsMap: Map<String, BibixValue>,
  ): BibixValue {
    val args = organizeParams(task, origin, ruleImplInfo.params, posParams, namedParamsMap)
    val methodName = ruleImplInfo.methodName ?: "build"
    val argsMap = args.toArgsMap()
    val inputHash = argsMap.extractInputHashes()
    val objectId = objectId {
      this.sourceId = origin.toProto(buildGraph)
      // TODO source_hash가 필요할까?
      // rule_impl에 대한 해시
      this.ruleImplIdHash = ruleImplInfo.implObjectIdHash
      this.methodName = methodName
      this.argsMap = argsMap
    }
    val objectDirectory = repo.prepareObjectDirectory(objectId, inputHash)
    val progressIndicator = getProgressIndicator()
    val buildContext = BuildContext(
      ruleImplInfo.origin,
      buildGraph.baseDirectories.getValue(ruleImplInfo.origin),
      buildGraph.baseDirectories.getValue(origin),
      repo.mainDirectory,
      args,
      objectDirectory.hashChanged,
      objectId,
      objectDirectory.objectIdHashHex,
      objectDirectory.directory,
      progressIndicator,
      repo,
    )

    // TODO instance 재활용이 필요할까?
    val instance = ruleImplInfo.cls.getDeclaredConstructor().newInstance()
    val method = ruleImplInfo.cls.getMethod(methodName, BuildContext::class.java)

    val callingTarget = "${ruleImplInfo.cls.canonicalName}:$methodName"

    return suspendCoroutine { cont ->
      check(method.trySetAccessible())
      progressIndicator.updateProgressDescription("Calling $callingTarget...")
      val invokeResult = try {
        method.invoke(instance, buildContext)
      } catch (e: Exception) {
        throw BibixBuildException(task, "Failed to invoke plugin", e)
      }
      progressIndicator.updateProgressDescription("Continuing from $callingTarget...")

      fun onFinalValue(result: BibixValue) {
        routineManager.executeSuspend(task, {
          coercer.coerce(task, origin, result, ruleImplInfo.returnType, ruleImplInfo.origin)
        }) { coerced ->
          if (coerced == null) {
            println(result)
            println(ruleImplInfo.returnType)
          }
          checkNotNull(coerced)
          synchronized(this) {
            repo.markFinished(objectId)
            // task의 objectId 저장
            taskObjectIds[task] = objectId
          }
          cont.resume(coerced)
        }
      }

      fun doNext(result: Any) {
        fun <T> nextCall(callback: (T) -> Any, result: T) {
          progressIndicator.updateProgressDescription("Calling (again) $callingTarget...")
          val nextResult = try {
            callback(result)
          } catch (e: Exception) {
            routineManager.markTaskFailed(BibixBuildException(task, "Plugin failed", e))
          }
          progressIndicator.updateProgressDescription("Continuing from $callingTarget...")
          doNext(nextResult)
        }
        when (result) {
          is BibixValue -> onFinalValue(result)
          is BuildRuleReturn.ValueReturn -> onFinalValue(result.value)
          is BuildRuleReturn.FailedReturn ->
            throw BibixBuildException(task, "Plugin failed", result.exception)
          is BuildRuleReturn.DoneReturn ->
            throw BibixBuildException(task, "build rule must not return Done value")
          is BuildRuleReturn.EvalAndThen -> {
            // `result.ruleName`을 . 단위로 끊어서 rule 이름을 찾은 다음
            val nameTokens = result.ruleName.split('.')
            val callingName = CName(ruleImplInfo.origin, nameTokens)
            // 해당 rule에 `result.params`을 줘서 실행하고
            routineManager.executeSuspend(task, {
              val calling = runTask(task, BuildTask.ResolveName(callingName))
              calling as BuildRuleImplInfo
              callBuildRule(
                task,
                calling.origin,
                calling,
                listOf(),
                result.params,
              )
            }) { buildResult ->
              // 결과를 `result.whenDone`으로 전달해서 반복
              nextCall(result.whenDone, buildResult)
            }
          }
          is BuildRuleReturn.GetClassInfos -> {
            val cnames = result.cnames + (result.unames.map { CName(origin, it) })
            routineManager.executeSuspend(task, {
              val resolvedClasses =
                cnames.map { runTask(task, BuildTask.ResolveName(it)) as CNameValue.ClassType }
              val realities = coercer.toTypeValues(task, resolvedClasses.map { it.reality })
              resolvedClasses.zip(realities)
            }) { zip ->
              val classDetails = zip.map { (cls, realityType) ->
                // TODO origin 입장에서 cls를 가리킬 때 어떻게 가리키면 되는지 넣어줘야 함
                TypeValue.ClassTypeDetail(cls.cname, cls.extendings, realityType)
              }
              nextCall(result.whenDone, classDetails)
            }
          }
        }
      }
      doNext(invokeResult)
    }
  }
}
