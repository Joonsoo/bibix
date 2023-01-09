package com.giyeok.bibix.runner

import com.giyeok.bibix.BibixIdProto
import com.giyeok.bibix.base.*
import com.giyeok.bibix.base.Constants.BIBIX_VERSION
import com.giyeok.bibix.buildscript.*
import com.giyeok.bibix.interpreter.AnyType
//import com.giyeok.bibix.interpreter.CustomType
import com.giyeok.bibix.interpreter.DirectoryType
import com.giyeok.bibix.interpreter.ListType
import com.giyeok.bibix.interpreter.coroutine.ProgressIndicator
import com.giyeok.bibix.interpreter.coroutine.ProgressIndicatorContainer
import com.giyeok.bibix.objectId
import com.giyeok.bibix.objectIdHash
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.repo.Repo
import com.giyeok.bibix.repo.extractInputHashes
import com.giyeok.bibix.repo.hashString
import com.giyeok.bibix.repo.toProto
import com.giyeok.bibix.source.ImportSourceResolver
import com.giyeok.bibix.source.ImportedSource
import com.giyeok.bibix.utils.toArgsMap
import com.giyeok.bibix.utils.toHexString
import com.giyeok.bibix.utils.toKtList
import com.google.protobuf.ByteString
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import org.codehaus.plexus.classworlds.ClassWorld
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.io.path.absolute

class BuildRunner(
  val buildEnv: BuildEnv,
  val buildGraph: BuildGraph,
  val rootScript: PreloadedPlugin,
  val preloadedPlugins: Map<String, PreloadedPlugin>,
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
  private val objectIdHashToTask = mutableMapOf<ByteString, BuildTask>()
  private val coercer = Coercer(buildGraph, this)

  private val realmIdCounter = AtomicInteger()
  fun nextRealmId(): String = "cw${realmIdCounter.incrementAndGet()}"

  private fun getProgressIndicator(): ProgressIndicator =
    progressIndicatorContainer.ofCurrentThread()

  private val imported = mutableMapOf<CNameValue.DeferredImport, SourceId>()
  private val resolvedNames = mutableMapOf<CName, BibixValue>()

  private val buildTaskRelGraph = BuildTaskRelGraph()

  private fun addSourceId(sourceId: BibixIdProto.SourceId): SourceId = when (sourceId.sourceCase) {
    BibixIdProto.SourceId.SourceCase.ROOT_SOURCE -> BibixRootSourceId
    BibixIdProto.SourceId.SourceCase.MAIN_SOURCE -> MainSourceId
    BibixIdProto.SourceId.SourceCase.BIBIX_INTERNAL_SOURCE -> PreloadedSourceId(sourceId.bibixInternalSource)
    // BibixIdProto.SourceId.SourceCase.LOCAL_SOURCE -> LocalSourceId(sourceId.localSource)
    BibixIdProto.SourceId.SourceCase.REMOTE_SOURCE -> {
      val remoteSourceId = buildGraph.addRemoteSource(sourceId.remoteSource)
      ExternSourceId(remoteSourceId)
    }

    else -> TODO()
  }

  fun getResolvedNameValue(name: CName) = synchronized(this) { resolvedNames[name] }

  fun getObjectIdOfTask(task: BuildTask): BibixIdProto.ObjectId? = taskObjectIds[task]
  fun getTaskByObjectIdHash(hash: ByteString): BuildTask? = objectIdHashToTask[hash]

  suspend fun runTasks(requestTask: BuildTask, tasks: List<BuildTask>) =
    coroutineScope {
      tasks.map { task ->
        async(currentCoroutineContext() + BuildTaskElement(task)) {
          runTask(requestTask, task)
        }
      }
    }.awaitAll()

  suspend fun runTask(requestTask: BuildTask, task: BuildTask): Any {
    if (routineManager.isTaskFinished(task)) {
      // 이미 결과가 나온 task를 다시 실행하면 기존 실행 결과 반환
      return routineManager.getTaskResult(task)!!
    }
    // requestTask가 실행되려면 task의 결과가 필요하다는 의미.
    // TODO 싸이클이 생기면 오류 발생하고 종료
    buildTaskRelGraph.addDependency(requestTask, task)
    val cycle = buildTaskRelGraph.findCycleBetween(requestTask, task)
    if (cycle != null) {
      throw BibixBuildException(task, "Cyclic dependency found: $cycle")
    }
    return coroutineScope {
      routineManager.asyncInvoke(task) {
        when (task) {
          is BuildTask.BuildRequest -> {
            // do nothing? 애당초 여기에 올 수가 있나?
          }

          is BuildTask.ResolveName -> resolveName(task)
          is BuildTask.ResolveImport -> resolveImport(task)
          is BuildTask.ResolveImportSource -> resolveImportSource(task)
          is BuildTask.EvalExpr -> evalExpr(task)
          is BuildTask.CallAction -> callAction(task)
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
      val parentValue = buildGraph.names.getValue(parent) as CNameValue.DeferredImport
      // import가 정상적으로 완료됐으면 import된 스크립트의 모든 element들은 buildGraph에 들어가 있어야 함
      when (val imported = loadImport(task, parentValue)) {
        is SourceId ->
          runTask(task, BuildTask.ResolveName(CName(imported, task.cname.tokens.drop(1))))

        is CNameValue.NamespaceValue ->
          runTask(task, BuildTask.ResolveName(imported.cname.append(task.cname.tokens.drop(1))))
        // import all일 때는 SourceId, import from일 때는 namespace value.
        // import from일 때 namespace 외에 다른 거일 수도 있나?
        else -> TODO()
      }
    } else {
      when (value) {
        is CNameValue.DeferredImport -> loadImport(task, value)
        is CNameValue.ExprValue -> {
          val evalTask = BuildTask.EvalExpr(
            task.cname.sourceId,
            value.exprGraphId,
            buildGraph.exprGraphs[value.exprGraphId].mainNode,
            null
          )
          val evalResult = runTask(task, evalTask) as BibixValue
          // CallExpr 실행시 evalTask -> target id 저장해두기
          synchronized(this) {
            resolvedNames[task.cname] = evalResult
            taskObjectIds[evalTask]?.let { objectId ->
              taskObjectIds[task] = objectId
              objectIdHashToTask[objectId.hashString()] = task
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
          val methodName = value.methodName ?: "build"
          // value.implName.sourceId is BibixRootSourceId 인 경우도 처리
          when {
            value.implName.sourceId == BibixRootSourceId -> {
              check(value.implName.tokens == listOf("$$"))
              BuildRuleImplInfo.NativeBuildRuleImplInfo(
                value,
                value.implName.sourceId,
                objectIdHash {
                  this.rootSource = "$BIBIX_VERSION:root:${value.className}"
                },
                rootScript.classes.getClass(value.className),
                methodName,
                value.params,
                value.returnType,
              )
            }

            value.implName.sourceId is PreloadedSourceId &&
              value.implName.tokens == listOf("$$") -> {
              // native impls
              val sourceId = value.implName.sourceId as PreloadedSourceId
              val plugin = preloadedPlugins.getValue(sourceId.name)
              BuildRuleImplInfo.NativeBuildRuleImplInfo(
                value,
                value.implName.sourceId,
                objectIdHash {
                  this.bibixInternalSource =
                    "$BIBIX_VERSION:internal ${sourceId.name}:${value.className}"
                },
                plugin.classes.getClass(value.className),
                methodName,
                value.params,
                value.returnType,
              )
            }

            else -> BuildRuleImplInfo.UserBuildRuleImplInfo(
              value,
              value.implName.sourceId,
              // objectIdHash { this.objectIdHashString = implObjectIdHash },
              value.implName,
              value.className,
              methodName,
              value.params,
              value.returnType
            )
          }
        }

        is CNameValue.ActionRuleValue -> {
          when {
            value.implName.sourceId is PreloadedSourceId &&
              value.implName.tokens == listOf("$$") -> {
              val sourceId = value.implName.sourceId as PreloadedSourceId
              val plugin = preloadedPlugins.getValue(sourceId.name)
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
                TODO(), // CustomType(CName(PreloadedSourceId("jvm"), "ClassPaths")),
                null
              )
              implResult as ClassInstanceValue
              val cps = ((implResult.fieldValues.getValue("cps")) as SetValue).values.map {
                (it as PathValue).path
              }
              val realm = synchronized(this) {
                val realm = classWorld.newRealm(nextRealmId())
                cps.forEach {
                  realm.addURL(it.absolute().toUri().toURL())
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

  private suspend fun loadImport(
    task: BuildTask.ResolveName,
    value: CNameValue.DeferredImport
  ): Any {
    val resolvedImport = synchronized(this) { imported[value] } ?: runTask(
      task,
      BuildTask.ResolveImport(task.cname.sourceId, value.deferredImportId)
    )
    when (resolvedImport) {
      is SourceId -> synchronized(this) {
        imported[value] = resolvedImport
      }

      is CNameValue.NamespaceValue -> synchronized(this) {
        imported[value] = resolvedImport.cname.sourceId
      }
    }
    return resolvedImport
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
        val bibixPlugin = preloadedPlugins[pluginName]
        checkNotNull(bibixPlugin) { "Unknown plugin: $pluginName" }
        val sourceId = PreloadedSourceId(pluginName)
        synchronized(this) {
          val defs = bibixPlugin.defs
          buildGraph.addDefs(
            sourceId,
            defs,
            // NameLookupContext(CName(sourceId), defs).withNative(),
            NameLookupContext(CName(BibixRootSourceId), rootScript.defs)
              .append(CName(sourceId), defs)
              .withNative(),
            repo.mainDirectory // default plugin은 base directory가 없음
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
            EnumValue("", "${result.cname}", exprNode.name)
          }

          is CNameValue.NamespaceValue -> {
            check(result.names.contains(exprNode.name))
            runTask(task, BuildTask.ResolveName(result.cname.append(exprNode.name)))
          }

          is NamedTupleValue -> result.getValue(exprNode.name)
          is ClassInstanceValue -> result.fieldValues.getValue(exprNode.name)
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
      is ExprNode.TypeCastNode -> {
        val targetResult = runTask(
          task,
          BuildTask.EvalExpr(task.origin, task.exprGraphId, exprNode.value, task.thisValue)
        )
        val targetValue = coercer.toBibixValue(task, targetResult)
        coercer.coerce(task, task.origin, targetValue, exprNode.type, null)
          ?: throw BibixBuildException(task, "Failed to cast")
      }
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
    val (cls: Class<*>, implObjectIdHash: BibixIdProto.ObjectIdHash) = when (ruleImplInfo) {
      is BuildRuleImplInfo.NativeBuildRuleImplInfo ->
        Pair(ruleImplInfo.cls, ruleImplInfo.implObjectIdHash)

      is BuildRuleImplInfo.UserBuildRuleImplInfo -> {
        // TODO implName을 여기서 resolve하지 말고 실제 실행하기 직전에 하기. 그래야 genRuleImplTempalte같은거 할때 싸이클이 안생김
        val implResolveTask = BuildTask.ResolveName(ruleImplInfo.implName)
        val implResult0 = runTask(task, implResolveTask)
        val implResult = coercer.coerce(
          task, origin, implResult0 as BibixValue,
          // TODO resolveClassPkgs 호출하도록 수정
          TODO(), // CustomType(CName(PreloadedSourceId("jvm"), "ClassPaths")),
          null
        )
        implResult as ClassInstanceValue
        synchronized(this) {
          val implObjectId = taskObjectIds.getValue(implResolveTask)
          val implObjectIdHash = implObjectId.hashString()
          val implObjectIdHashHex = implObjectIdHash.toHexString()
          // implResult의 objectId가 같으면 realm 재사용
          val realm = try {
            classWorld.getRealm(implObjectIdHashHex)
          } catch (e: NoSuchRealmException) {
            val newRealm = classWorld.newRealm(implObjectIdHashHex)
            val cps = ((implResult.fieldValues.getValue("cps")) as SetValue).values.map {
              (it as PathValue).path
            }
            cps.forEach {
              newRealm.addURL(it.absolute().toUri().toURL())
            }
            newRealm
          }
          Pair(
            realm.loadClass(ruleImplInfo.className),
            objectIdHash { this.objectIdHashString = implObjectIdHash })
        }
      }
    }
    val args = organizeParams(task, origin, ruleImplInfo.params, posParams, namedParamsMap)
    val methodName = ruleImplInfo.methodName
    val argsMap = args.toArgsMap()
    val inputHash = argsMap.extractInputHashes()
    val objectId = objectId {
      this.sourceId = origin.toProto(buildGraph)
      // TODO source_hash가 필요할까?
      // rule_impl에 대한 해시
      this.ruleImplIdHash = implObjectIdHash
      this.methodName = methodName
      this.argsMap = argsMap
    }
    val objectDirectory = repo.prepareObjectDirectory(objectId, inputHash)
    val progressIndicator = getProgressIndicator()
    val buildContext = BuildContext(
      buildEnv,
      ruleImplInfo.origin,
      repo.fileSystem,
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
    val instance = cls.getDeclaredConstructor().newInstance()
    val method = cls.getMethod(methodName, BuildContext::class.java)

    val callingTarget = "${cls.canonicalName}:$methodName"

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
            routineManager.markTaskFailed(
              BibixBuildException(
                task,
                "Failed to coerce return value, value=$result, returnType=${ruleImplInfo.returnType}"
              )
            )
          }
          checkNotNull(coerced)
          synchronized(this) {
            repo.markFinished(objectId)
            // task의 objectId 저장
            taskObjectIds[task] = objectId
            objectIdHashToTask[objectId.hashString()] = task
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
                ruleImplInfo.origin,
                calling,
                listOf(),
                result.params,
              )
            }) { buildResult ->
              // 결과를 `result.whenDone`으로 전달해서 반복
              nextCall(result.whenDone, buildResult)
            }
          }

          is BuildRuleReturn.GetClassTypeDetails -> {
            val cnames = result.cnames + (result.unames.map { CName(origin, it) })
            routineManager.executeSuspend(task, {
              val resolvedClasses =
                cnames.map { runTask(task, BuildTask.ResolveName(it)) as CNameValue.ClassType }
              resolvedClasses.map { cls ->
                val relativeName = if (cls.cname.sourceId == origin) {
                  cls.cname.tokens
                } else {
                  // TODO 이거 제대로 안될거같은데?
                  val importSource = imported.entries.find { it.value == cls.cname.sourceId }!!
                  val importer = buildGraph.names.entries.find { it.value == importSource.key }!!
                  listOf(importer.key.tokens.first()) + cls.cname.tokens
                }

                when (cls) {
                  is CNameValue.DataClassType -> {
                    val fieldTypeValues = coercer.toTypeValues(task, cls.fields.map { it.type })
                    TypeValue.DataClassTypeDetail(
                      "",
                      "${cls.cname}",
                      cls.fields.zip(fieldTypeValues).map {
                        TypeValue.DataClassFieldValue(
                          it.first.name,
                          it.second,
                          it.first.optional
                        )
                      })
                  }

                  is CNameValue.SuperClassType -> {
//                    val subNames = cls.subs.map { it.name }
//                    val subTypes = runTasks(task, subNames.map { BuildTask.ResolveName(it) })
//                    println(subTypes)
//                    TypeValue.SuperClassTypeDetail("", "${cls.cname}", listOf())
                    TODO()
                  }
                }
              }
            }) { resolved ->
              nextCall(result.whenDone, resolved)
            }
          }
        }
      }
      doNext(invokeResult)
    }
  }

  private suspend fun callAction(task: BuildTask.CallAction): Any {
    val exprGraph = buildGraph.exprGraphs[task.exprGraphId]
    val callExpr = exprGraph.callExprs[(exprGraph.mainNode as ExprNode.CallExprNode).callExprId]
    val targetTask = BuildTask.EvalExpr(task.origin, task.exprGraphId, callExpr.target, null)
    val namedParams = callExpr.params.namedParams.entries.toList().sortedBy { it.key }
    val paramTasks = (callExpr.params.posParams + namedParams.map { it.value })
      .map { BuildTask.EvalExpr(task.origin, task.exprGraphId, it, null) }
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

    val actionContext = ActionContext(buildEnv, ruleImplInfo.origin, args, progressIndicator)

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
}
