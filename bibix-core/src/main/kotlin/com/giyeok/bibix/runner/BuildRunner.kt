package com.giyeok.bibix.runner

import com.giyeok.bibix.base.*
import com.giyeok.bibix.buildscript.*
import com.giyeok.bibix.runner.Constants.BIBIX_VERSION
import com.giyeok.bibix.utils.ProgressIndicator
import com.giyeok.bibix.utils.toArgsMap
import com.giyeok.bibix.utils.toHexString
import com.giyeok.bibix.utils.toKtList
import org.codehaus.plexus.classworlds.ClassWorld
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException
import java.io.File
import kotlin.system.exitProcess

class BuildRunner(
  val buildGraph: BuildGraph,
  val rootScript: BibixPlugin,
  val bibixPlugins: Map<String, BibixPlugin>,
  val repo: Repo,
  val bibixArgs: Map<CName, BibixValue> = mapOf(),
  val actionArgs: ListValue? = null,
  val classWorld: ClassWorld = ClassWorld(),
  val routinesLogger: BuildTaskRoutineLogger = BuildTaskRoutineLoggerImpl(),
) {
  private val importSourceResolver = ImportSourceResolver(repo)

  private val taskObjectIds = mutableMapOf<BuildTask, BibixIdProto.ObjectId>()

  val routinesManager = BuildTaskRoutinesManager(this::runTasks, routinesLogger)

  fun runTargets(targets: Collection<CName>) {
    runTasks(targets.map { BuildTask.ResolveName(it) })
  }

  fun markTaskFailed(task: BuildTask, exception: Exception): Exception {
    // TODO
    when (task) {
      is BuildTask.ExprEval -> {
        val exprGraph = buildGraph.exprGraphs[task.exprGraphId]
        val (sourceId, parseNode) = exprGraph.exprLocation
        println("@ $sourceId, ${parseNode.range()}")
        println(parseNode.sourceText())
      }
      is BuildTask.CallAction -> {
        val exprGraph = buildGraph.exprGraphs[task.exprGraphId]
        val (sourceId, parseNode) = exprGraph.exprLocation
        println("@ $sourceId, ${parseNode.range()}")
        println(parseNode.sourceText())
      }
      else -> {
        // do nothing
      }
    }
    exception.printStackTrace()
    exitProcess(1)
    return exception
  }

  fun registerTaskResult(task: BuildTask, value: Any) {
    // task의 결과가 value로 완료되었으니 task에 depend중인 태스크들을 이어서 실행
    routinesManager.registerTaskResult(task, value)
  }

  // task를 실행하려면 requiring이 먼저 수행되어야 하고 requiring이 끝나면 onReady 실행
  fun require(
    task: BuildTask,
    requiring: BuildTask,
    onReady: (Any, ProgressIndicator<BuildTaskRoutineId>) -> Unit
  ) {
    require(task, listOf(requiring)) { results, pi ->
      onReady(results.first(), pi)
    }
  }

  fun require(
    task: BuildTask,
    requirings: List<BuildTask>,
    onReady: (List<Any>, ProgressIndicator<BuildTaskRoutineId>) -> Unit
  ) {
    // requirings 중 이미 완료되었거나 돌고있는 태스크는 실행하지 않기
    routinesManager.require(task, requirings, onReady)
  }

  fun runTasks(tasks: Collection<BuildTask>) {
    tasks.forEach { runTask(it) }
  }

  data class BuildRuleImplInfo(
    val origin: SourceId,
    val implObjectIdHash: BibixIdProto.ObjectIdHash,
    val cls: Class<*>,
    val methodName: String?,
    val params: List<Param>,
    val returnType: BibixType
  )

  data class ActionRuleImplInfo(
    val origin: SourceId,
    val cls: Class<*>,
    val methodName: String?,
    val params: List<Param>,
  )

  private var _nextRealmId: Int = 0
  fun nextRealmId(): String {
    _nextRealmId += 1
    return "cw$_nextRealmId"
  }

  private fun organizeParams(
    task: BuildTask,
    origin: SourceId,
    paramDefs: List<Param>,
    posParams: List<BibixValue>,
    namedParams: Map<String, BibixValue>,
    onFinished: (Map<String, BibixValue>?) -> Unit,
  ) {
    val posParamsZipped = paramDefs.zip(posParams)
    val remainingParams = paramDefs.drop(posParams.size)
    val invalidParams = namedParams.keys - remainingParams.map { it.name }.toSet()
    if (invalidParams.isNotEmpty()) {
      throw markTaskFailed(task, Exception("Invalid parameter name: $invalidParams"))
    }

    val paramDefsMap = paramDefs.associateBy { it.name }
    val posParamsMap = posParamsZipped.associate { it.first.name to it.second }

    val unspecifiedParams = paramDefsMap.keys - posParamsMap.keys - namedParams.keys

    val missingParams = unspecifiedParams.filter {
      val paramDef = paramDefsMap.getValue(it)
      !paramDef.optional && paramDef.defaultValueId == null
    }
    if (missingParams.isNotEmpty()) {
      throw markTaskFailed(task, IllegalArgumentException("Missing parameters: $missingParams"))
    }

    val defaults = unspecifiedParams.mapNotNull {
      val paramDef = paramDefsMap.getValue(it)
      if (paramDef.defaultValueId == null) null else paramDef.name to paramDef.defaultValueId
    }

    fun recDefaults(
      defaults: List<Pair<String, Int>>,
      cc: MutableMap<String, BibixValue>,
      onFinished: (Map<String, BibixValue>) -> Unit
    ) {
      if (defaults.isEmpty()) {
        onFinished(cc)
      } else {
        val next = defaults.first()
        val exprGraph = buildGraph.exprGraphs[next.second]
        require(
          task, BuildTask.ExprEval(origin, next.second, exprGraph.mainNode, null)
        ) { defaultValue, _ ->
          cc[next.first] = defaultValue as BibixValue
          recDefaults(defaults.drop(1), cc, onFinished)
        }
      }
    }

    fun recCoerce(
      params: Map<String, BibixValue>, queue: List<String>, cc: MutableMap<String, BibixValue>
    ) {
      if (queue.isEmpty()) {
        onFinished(cc)
      } else {
        val next = queue.first()
        coerce(
          task,
          origin,
          params.getValue(next),
          paramDefsMap.getValue(next).type,
          null
        ) { coerced ->
          if (coerced == null) {
            onFinished(null)
          } else {
            cc[next] = coerced
            recCoerce(params, queue.drop(1), cc)
          }
        }
      }
    }

    recDefaults(defaults, (posParamsMap + namedParams).toMutableMap()) { params ->
      recCoerce(params, params.keys.toList(), mutableMapOf())
    }
  }

  private fun runTask(task: BuildTask) {
    when (task) {
      is BuildTask.ResolveName -> {
        val value = buildGraph.names[task.cname]
        if (value == null) {
          // buildGraph.names에 task.cname이 없으면 그 parent 중에 있는것을 찾아서 처리한 다음 진행
          var parent = task.cname.parent()
          while (!buildGraph.names.containsKey(parent)) {
            if (parent.tokens.isEmpty()) {
              throw markTaskFailed(task, IllegalStateException("Cannot find name ${task.cname}"))
            }
            parent = parent.parent()
          }
          val parentValue = buildGraph.names.getValue(parent)
          if (parentValue is CNameValue.DeferredImport) {
            require(
              task, BuildTask.ResolveImport(task.cname.sourceId, parentValue.deferredImportId)
            ) { importedSourceId, _ ->
              // import가 정상적으로 완료됐으면 import된 스크립트의 모든 element들은 buildGraph에 들어가 있어야 함
              when (importedSourceId) {
                is SourceId -> {
                  synchronized(this) {
                    buildGraph.replaceValue(parent, CNameValue.LoadedImport(importedSourceId))
                  }
                  require(
                    task, BuildTask.ResolveName(CName(importedSourceId, task.cname.tokens.drop(1)))
                  ) { taskResult, _ ->
                    registerTaskResult(task, taskResult)
                  }
                }
                is CNameValue.NamespaceValue -> {
                  require(
                    task,
                    BuildTask.ResolveName(importedSourceId.cname.append(task.cname.tokens.drop(1)))
                  ) { taskResult, _ ->
                    registerTaskResult(task, taskResult)
                  }
                }
                // import all일 때는 SourceId, import from일 때는 namespace value.
                // import from일 때 namespace 외에 다른 거일 수도 있나?
              }
            }
          } else {
            parentValue as CNameValue.LoadedImport
            require(
              task, BuildTask.ResolveName(CName(parentValue.sourceId, task.cname.tokens.drop(1)))
            ) { taskResult, _ ->
              registerTaskResult(task, taskResult)
            }
          }
        } else {
          when (value) {
            is CNameValue.LoadedImport -> registerTaskResult(task, value)
            is CNameValue.DeferredImport -> {
              require(
                task, BuildTask.ResolveImport(task.cname.sourceId, value.deferredImportId)
              ) { importResult, _ ->
                // TODO buildGraph.replaceValue(task.cname, importResult)
                registerTaskResult(task, importResult)
              }
            }
            is CNameValue.ExprValue -> {
              val evalTask = BuildTask.ExprEval(
                task.cname.sourceId,
                value.exprGraphId,
                buildGraph.exprGraphs[value.exprGraphId].mainNode,
                null
              )
              require(task, evalTask) { evalResult, _ ->
                buildGraph.replaceValue(
                  task.cname, CNameValue.EvaluatedValue(evalResult as BibixValue)
                )
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
                registerTaskResult(task, evalResult)
              }
            }
            is CNameValue.EvaluatedValue -> registerTaskResult(task, value.value)
            is CNameValue.NamespaceValue -> registerTaskResult(task, value)
            is CNameValue.ClassType -> registerTaskResult(task, value)
            is CNameValue.EnumType -> registerTaskResult(task, value)
            is CNameValue.ArgVar -> {
              // 사용자가 지정한 arg가 있으면 그 값을 반환하고
              // 없으면 default value를 evaluate
              val userArg = bibixArgs[task.cname]
              if (userArg != null) {
                registerTaskResult(task, userArg)
              } else {
                if (value.defaultValueId == null) {
                  throw markTaskFailed(
                    task,
                    IllegalStateException("argument not specified: ${task.cname}")
                  )
                }
                val exprGraph = buildGraph.exprGraphs[value.defaultValueId]
                require(
                  task, BuildTask.ExprEval(
                    task.cname.sourceId, value.defaultValueId, exprGraph.mainNode, null
                  )
                ) { argValue, _ ->
                  registerTaskResult(task, argValue)
                }
              }
            }
            is CNameValue.BuildRuleValue -> {
              // value.implName.sourceId is BibixRootSourceId 인 경우도 처리
              when {
                value.implName.sourceId == BibixRootSourceId -> {
                  check(value.implName.tokens == listOf("$$"))
                  val ruleImplInfo = BuildRuleImplInfo(
                    value.implName.sourceId,
                    objectIdHash {
                      this.rootSource = "$BIBIX_VERSION:root:${value.className}"
                    },
                    rootScript.classes.getClass(value.className),
                    value.methodName,
                    value.params,
                    value.returnType,
                  )
                  registerTaskResult(task, ruleImplInfo)
                }
                value.implName.sourceId is BibixInternalSourceId &&
                  value.implName.tokens == listOf("$$") -> {
                  // native impls
                  val plugin = bibixPlugins.getValue(value.implName.sourceId.name)
                  val ruleImplInfo = BuildRuleImplInfo(
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
                  registerTaskResult(task, ruleImplInfo)
                }
                else -> {
                  val implResolveTask = BuildTask.ResolveName(value.implName)
                  require(task, implResolveTask) { implResult0, _ ->
                    coerce(
                      task, task.cname.sourceId, implResult0 as BibixValue,
                      // TODO resolveClassPkgs 호출하도록 수정
                      CustomType(CName(BibixInternalSourceId("jvm"), "ClassPaths")),
                      null
                    ) { implResult ->
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
                          value.implName.sourceId,
                          objectIdHash { this.objectIdHashString = implObjectIdHash },
                          realm.loadClass(value.className),
                          value.methodName,
                          value.params,
                          value.returnType
                        )
                      }
                      // TODO buildGraph.replaceValue(task.cname, implResult)
                      registerTaskResult(task, ruleImplInfo)
                    }
                  }
                }
              }
            }
            is CNameValue.ActionRuleValue -> {
              when {
                value.implName.sourceId is BibixInternalSourceId &&
                  value.implName.tokens == listOf("$$") -> {
                  val plugin = bibixPlugins.getValue(value.implName.sourceId.name)
                  val actionImplInfo = ActionRuleImplInfo(
                    value.implName.sourceId,
                    plugin.classes.getClass(value.className),
                    value.methodName,
                    value.params
                  )
                  registerTaskResult(task, actionImplInfo)
                }
                else -> {
                  require(task, BuildTask.ResolveName(value.implName)) { implResult0, _ ->
                    coerce(
                      task,
                      task.cname.sourceId,
                      implResult0 as BibixValue,
                      CustomType(CName(BibixInternalSourceId("jvm"), "ClassPaths")),
                      null
                    ) { implResult ->
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
                        value.implName.sourceId,
                        realm.loadClass(value.className),
                        value.methodName,
                        value.params,
                      )
                      // TODO buildGraph.replaceValue(task.cname, implResult)
                      registerTaskResult(task, actionImplInfo)
                    }
                  }
                }
              }
            }
            is CNameValue.ActionCallValue -> require(
              task,
              BuildTask.CallAction(task.cname.sourceId, value.exprGraphId)
            ) { _, _ ->
              registerTaskResult(task, NoneValue)
            }
          }
        }
      }
      is BuildTask.CallAction -> {
        val exprGraph = buildGraph.exprGraphs[task.exprGraphId]
        val callExpr = exprGraph.callExprs[(exprGraph.mainNode as ExprNode.CallExprNode).callExprId]
        val targetTask = BuildTask.ExprEval(task.origin, task.exprGraphId, callExpr.target, null)
        val namedParams = callExpr.params.namedParams.entries.toList().sortedBy { it.key }
        val paramTasks = (callExpr.params.posParams + namedParams.map { it.value }).map {
          BuildTask.ExprEval(
            task.origin,
            task.exprGraphId,
            it,
            null
          )
        }
        require(task, listOf(targetTask) + paramTasks) { prerequisites, progressIndicator ->
          val ruleImplInfo = prerequisites[0] as ActionRuleImplInfo

          val instance = ruleImplInfo.cls.getDeclaredConstructor().newInstance()
          val method =
            ruleImplInfo.cls.getMethod(ruleImplInfo.methodName ?: "run", ActionContext::class.java)

          val paramValues = prerequisites.drop(1).map { it as BibixValue }
          val posParams = paramValues.take(callExpr.params.posParams.size)
          val namedParamsMap =
            namedParams.map { it.key }.zip(paramValues.drop(callExpr.params.posParams.size)).toMap()
          organizeParams(
            task, task.origin, ruleImplInfo.params, posParams, namedParamsMap
          ) { args ->
            if (args == null) {
              throw markTaskFailed(task, Exception("Failed to get arguments"))
            }
            val context = ActionContext(ruleImplInfo.origin, args)

            val invokeResult = try {
              method.invoke(instance, context)
            } catch (e: Exception) {
              throw markTaskFailed(task, e)
            }

            fun doNext(result: Any?) {
              when (result) {
                null -> registerTaskResult(task, NoneValue)
                is BuildRuleReturn.ValueReturn ->
                  throw markTaskFailed(
                    task,
                    IllegalStateException("action rule must not return a value")
                  )
                is BuildRuleReturn.FailedReturn -> throw markTaskFailed(task, result.exception)
                is BuildRuleReturn.DoneReturn -> registerTaskResult(task, NoneValue)
                is BuildRuleReturn.EvalAndThen -> {
                  // `result.ruleName`을 . 단위로 끊어서 rule 이름을 찾은 다음
                  val nameTokens = result.ruleName.split('.')
                  val callingName = CName(ruleImplInfo.origin, nameTokens)
                  // 해당 rule에 `result.params`을 줘서 실행하고
                  require(task, BuildTask.ResolveName(callingName)) { calling, _ ->
                    calling as BuildRuleImplInfo
                    callBuildRule(
                      task,
                      calling.origin,
                      calling,
                      listOf(),
                      result.params,
                      progressIndicator,
                    ) { buildResult ->
                      // 결과를 `result.whenDone`으로 전달해서 반복
                      try {
                        doNext(result.whenDone(buildResult))
                      } catch (e: Exception) {
                        throw markTaskFailed(task, e)
                      }
                    }
                  }
                }
              }
            }
            doNext(invokeResult)
          }
        }
      }
      is BuildTask.ExprEval -> {
        val exprGraph = buildGraph.exprGraphs[task.exprGraphId]
        when (val exprNode = task.exprNode) {
          is ExprNode.CallExprNode -> {
            val callExpr = exprGraph.callExprs[exprNode.callExprId]
            val implTask =
              BuildTask.ExprEval(task.origin, task.exprGraphId, callExpr.target, task.thisValue)
            require(task, implTask) { ruleImplInfo, progressIndicator ->
              ruleImplInfo as BuildRuleImplInfo
              val namedParams = callExpr.params.namedParams.entries.toList().sortedBy { it.key }
              val paramTasks = (callExpr.params.posParams + namedParams.map { it.value }).map {
                BuildTask.ExprEval(
                  task.origin,
                  task.exprGraphId,
                  it,
                  task.thisValue
                )
              }
              require(task, paramTasks) { params, _ ->
                val paramValues = params.map { it as BibixValue }
                val posParams = paramValues.take(callExpr.params.posParams.size)
                val namedParamsMap =
                  namedParams.map { it.key }.zip(paramValues.drop(callExpr.params.posParams.size))
                    .toMap()
                callBuildRule(
                  task, task.origin, ruleImplInfo, posParams, namedParamsMap, progressIndicator
                ) { buildResult ->
                  registerTaskResult(task, buildResult)
                }
              }
            }
          }

          is ExprNode.NameNode -> require(task, BuildTask.ResolveName(exprNode.name)) { result, _ ->
            registerTaskResult(task, result)
          }
          is ExprNode.MergeOpNode -> require(
            task, listOf(
              BuildTask.ExprEval(task.origin, task.exprGraphId, exprNode.lhs, task.thisValue),
              BuildTask.ExprEval(task.origin, task.exprGraphId, exprNode.rhs, task.thisValue)
            )
          ) { results, _ ->
            val lhs = results[0] as BibixValue
            val rhs = results[1] as BibixValue
            // lhs와 rhs를 각각 list로 coerce
            coerce(task, task.origin, lhs, ListType(AnyType), null) { lhsList ->
              lhsList as ListValue
              coerce(task, task.origin, rhs, ListType(AnyType), null) { rhsList ->
                rhsList as ListValue
                registerTaskResult(task, ListValue(lhsList.values + rhsList.values))
              }
            }
          }
          is ExprNode.MemberAccessNode -> require(
            task, BuildTask.ExprEval(task.origin, task.exprGraphId, exprNode.target, task.thisValue)
          ) { result, _ ->
            when (result) {
              is CNameValue.EnumType -> {
                check(result.values.contains(exprNode.name))
                registerTaskResult(task, EnumValue(result.cname, exprNode.name))
              }
              is CNameValue.NamespaceValue -> {
                check(result.names.contains(exprNode.name))
                require(
                  task, BuildTask.ResolveName(result.cname.append(exprNode.name))
                ) { resolved, _ ->
                  registerTaskResult(task, resolved)
                }
              }
              is BibixValue -> {
                fun access(value: BibixValue, name: String): BibixValue = when (value) {
                  is NamedTupleValue -> value.getValue(name)
                  is ClassInstanceValue -> access(value.value, name)
                  else ->
                    throw markTaskFailed(task, IllegalStateException("Cannot access $value $name"))
                }
                registerTaskResult(task, access(result, exprNode.name))
              }
              is SourceId -> {
                require(task, BuildTask.ResolveName(CName(result, exprNode.name))) { resolved, _ ->
                  registerTaskResult(task, resolved)
                }
              }
              else -> throw markTaskFailed(task, IllegalStateException("Invalid access: $exprNode"))
            }
          }

          is ExprNode.ListNode -> {
            require(task, exprNode.elems.map {
              BuildTask.ExprEval(task.origin, task.exprGraphId, it, task.thisValue)
            }) { elemResults, _ ->
              registerTaskResult(task, ListValue(elemResults.map { it as BibixValue }))
            }
          }
          is ExprNode.TupleNode -> {
            require(task, exprNode.elems.map {
              BuildTask.ExprEval(task.origin, task.exprGraphId, it, task.thisValue)
            }) { elemResults, _ ->
              registerTaskResult(task, TupleValue(elemResults.map { it as BibixValue }))
            }
          }
          is ExprNode.NamedTupleNode -> {
            require(task, exprNode.elems.map {
              BuildTask.ExprEval(task.origin, task.exprGraphId, it.second, task.thisValue)
            }) { elemResults, _ ->
              registerTaskResult(
                task,
                NamedTupleValue(exprNode.elems.zip(elemResults)
                  .map { it.first.first to it.second as BibixValue })
              )
            }
          }
          is ExprNode.StringLiteralNode -> {
            require(task, exprNode.stringElems.filterIsInstance<ExprChunk>().map {
              BuildTask.ExprEval(task.origin, task.exprGraphId, it.expr, task.thisValue)
            }) { results, _ ->
              val stringBuilder = StringBuilder()
              var i = 0
              exprNode.stringElems.forEach { elem ->
                when (elem) {
                  is ExprChunk -> {
                    stringBuilder.append((results[i] as BibixValue).stringify())
                    i += 1
                  }
                  is StringChunk -> stringBuilder.append(elem.value)
                }
              }
              registerTaskResult(task, StringValue(stringBuilder.toString()))
            }
          }
          is ExprNode.BooleanLiteralNode -> registerTaskResult(task, BooleanValue(exprNode.value))

          is ExprNode.ClassThisRef -> {
            registerTaskResult(task, checkNotNull(task.thisValue))
          }
          ExprNode.ActionArgsRef -> registerTaskResult(task, actionArgs!!)
        }
      }
      is BuildTask.ResolveImportSource -> {
        when (val source = task.importSource) {
          is ImportSource.ImportSourceCall -> {
            val exprGraph = buildGraph.exprGraphs[source.sourceExprId]
            require(
              task, BuildTask.ExprEval(source.origin, source.sourceExprId, exprGraph.mainNode, null)
            ) { result, progressIndicator ->
              // git과 같은 "root" source에 있는 함수들은 named tuple로 소스에 대한 정보를 반환하고 여기서는 그 값을 받아서 사용

              val importedSource = importSourceResolver.resolveImportSourceCall(
                result as ClassInstanceValue, progressIndicator
              )
              registerTaskResult(task, importedSource)
            }
          }
          is ImportSource.ImportSourceString -> {
            val exprGraph = buildGraph.exprGraphs[source.stringLiteralExprId]
            require(
              task, BuildTask.ExprEval(
                source.origin, source.stringLiteralExprId, exprGraph.mainNode, null
              )
            ) { result, progressIndicator ->
              coerce(
                task,
                task.origin,
                result as StringValue,
                DirectoryType,
                null
              ) { importDirectory ->
                val importedSource = importSourceResolver.resolveImportSourcePath(
                  repo.mainDirectory, importDirectory as DirectoryValue, progressIndicator
                )
                registerTaskResult(task, importedSource)
              }
            }
          }
        }
      }
      is BuildTask.ResolveImport -> {
        when (val importDef = buildGraph.deferredImports[task.importDefId]) {
          is DeferredImportDef.ImportAll -> {
            require(
              task, BuildTask.ResolveImportSource(task.origin, importDef.importSource)
            ) { importedSource, _ ->
              importedSource as ImportedSource
              synchronized(this) {
                val sourceId = addSourceId(importedSource.sourceId)
                // TODO add to buildGraph
                buildGraph.addDefs(
                  sourceId,
                  importedSource.buildScript.defs().toKtList(),
                  importedSource.baseDirectory,
                )
                registerTaskResult(task, sourceId)
              }
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
              registerTaskResult(task, sourceId)
            }
          }
          is DeferredImportDef.ImportFrom -> {
            require(
              task, BuildTask.ResolveImportSource(task.origin, importDef.importSource)
            ) { importedSource, _ ->
              importedSource as ImportedSource
              synchronized(this) {
                val sourceId = addSourceId(importedSource.sourceId)
                buildGraph.addDefs(
                  sourceId,
                  importedSource.buildScript.defs().toKtList(),
                  importedSource.baseDirectory
                )
                // println(importedSource)
                require(
                  task,
                  BuildTask.ResolveName(CName(sourceId, importDef.nameTokens))
                ) { importing, _ ->
                  registerTaskResult(task, importing)
                }
              }
            }
          }
          is DeferredImportDef.ImportMainSub -> {
            require(
              task, BuildTask.ResolveName(CName(MainSourceId, importDef.nameTokens))
            ) { resolved, _ ->
              registerTaskResult(task, resolved)
            }
          }
        }
      }
    }
  }

  private fun callBuildRule(
    task: BuildTask,
    origin: SourceId,
    ruleImplInfo: BuildRuleImplInfo,
    posParams: List<BibixValue>,
    namedParamsMap: Map<String, BibixValue>,
    progressIndicator: ProgressIndicator<BuildTaskRoutineId>,
    whenDone: (BibixValue) -> Unit
  ) {
    organizeParams(
      task, origin, ruleImplInfo.params, posParams, namedParamsMap
    ) { args ->
      if (args == null) {
        throw markTaskFailed(task, Exception("Failed to get arguments"))
      }
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
      val context = BuildContext(
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

      check(method.trySetAccessible())
      val invokeResult = try {
        method.invoke(instance, context)
      } catch (e: Exception) {
        throw markTaskFailed(task, e)
      }

      fun onFinalValue(result: BibixValue) {
        coerce(task, origin, result, ruleImplInfo.returnType, ruleImplInfo.origin) { coerced ->
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
          whenDone(coerced)
        }
      }

      fun doNext(result: Any) {
        when (result) {
          is BibixValue -> onFinalValue(result)
          is BuildRuleReturn.ValueReturn -> onFinalValue(result.value)
          is BuildRuleReturn.FailedReturn -> throw markTaskFailed(task, result.exception)
          is BuildRuleReturn.DoneReturn ->
            throw markTaskFailed(
              task,
              IllegalStateException("build rule must not return Done value")
            )
          is BuildRuleReturn.EvalAndThen -> {
            // `result.ruleName`을 . 단위로 끊어서 rule 이름을 찾은 다음
            val nameTokens = result.ruleName.split('.')
            val callingName = CName(ruleImplInfo.origin, nameTokens)
            // 해당 rule에 `result.params`을 줘서 실행하고
            require(task, BuildTask.ResolveName(callingName)) { calling, _ ->
              calling as BuildRuleImplInfo
              callBuildRule(
                task,
                calling.origin,
                calling,
                listOf(),
                result.params,
                progressIndicator,
              ) { buildResult ->
                // 결과를 `result.whenDone`으로 전달해서 반복
                try {
                  doNext(result.whenDone(buildResult))
                } catch (e: Exception) {
                  throw markTaskFailed(task, e)
                }
              }
            }
          }
        }
      }
      doNext(invokeResult)
    }
  }

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

  private fun fileFromString(origin: SourceId, path: String): File {
    val base = buildGraph.baseDirectories.getValue(origin)
    return File(base, path)
  }

  fun coerce(
    task: BuildTask,
    origin: SourceId,
    value: BibixValue,
    type: BibixType,
    dclassOrigin: SourceId?,
    onFinished: (BibixValue?) -> Unit,
  ) {
    if (value is NClassInstanceValue) {
      // ClassInstanceValue로 변환
      checkNotNull(dclassOrigin)
      require(task, BuildTask.ResolveName(CName(dclassOrigin, value.nameTokens))) { resolved, _ ->
        resolved as CNameValue.ClassType
        coerce(
          task,
          origin,
          ClassInstanceValue(resolved.cname, value.value),
          type,
          dclassOrigin,
          onFinished,
        )
      }
    } else {
      // TODO value is ClassInstanceValue 인데 type은 ClassType이 아닌 경우
      // -> class의 cast 함수 시도 -> value.reality를 coerce
      when (type) {
        AnyType -> onFinished(value)
        BooleanType -> when (value) {
          is BooleanValue -> onFinished(value)
          else -> onFinished(null)
        }
        StringType -> when (value) {
          is StringValue -> onFinished(value)
          else -> onFinished(StringValue(value.stringify()))
        }
        PathType -> when (value) {
          is PathValue -> onFinished(value)
          is FileValue -> onFinished(PathValue(value.file))
          is DirectoryValue -> onFinished(PathValue(value.directory))
          is StringValue -> {
            // TODO 이 value의 root 디렉토리를 베이스로
            onFinished(PathValue(fileFromString(origin, value.value)))
          }
          is ClassInstanceValue -> {
            // TODO cast 규칙이 있으면 먼저 시도해야할듯? -> 그런데 지금은 문법에서 class to class cast만 되는것같은데?
            coerce(task, origin, value.value, type, dclassOrigin, onFinished)
          }
          else -> onFinished(null)
        }
        FileType -> when (value) {
          is FileValue -> onFinished(value)
          is PathValue -> {
            check(value.path.exists() && value.path.isFile)
            onFinished(FileValue(value.path))
          }
          is StringValue -> {
            // TODO 이 value의 origin의 root 디렉토리를 베이스로
            val file = fileFromString(origin, value.value)
            check(file.exists() && file.isFile)
            onFinished(FileValue(file))
          }
          else -> onFinished(null)
        }
        DirectoryType -> when (value) {
          is DirectoryValue -> onFinished(value)
          is PathValue -> {
            check(value.path.exists() && value.path.isDirectory)
            onFinished(DirectoryValue(value.path))
          }
          is StringValue -> {
            // TODO 이 value의 origin의 root 디렉토리를 베이스로
            val file = fileFromString(origin, value.value)
            check(file.exists() && file.isDirectory)
            onFinished(DirectoryValue(file))
          }
          else -> onFinished(null)
        }
        is CustomType -> {
          require(task, BuildTask.ResolveName(type.name)) { actualType, _ ->
            when (actualType) {
              is CNameValue.ClassType -> {
                when (value) {
                  is ClassInstanceValue -> {
                    if (actualType.cname == value.className) {
                      coerce(
                        task,
                        origin,
                        value.value,
                        actualType.reality,
                        dclassOrigin
                      ) { coerced ->
                        if (coerced == null) {
                          onFinished(null)
                        } else {
                          onFinished(ClassInstanceValue(actualType.cname, coerced))
                        }
                      }
                    } else {
                      // type casting
                      fun tryCoerceToReality() {
                        coerce(task, origin, value, actualType.reality, dclassOrigin) { coerced ->
                          if (coerced == null) {
                            onFinished(null)
                          } else {
                            onFinished(ClassInstanceValue(actualType.cname, coerced))
                          }
                        }
                      }
                      require(task, BuildTask.ResolveName(value.className)) { valueType, _ ->
                        valueType as CNameValue.ClassType
                        val castExprId = valueType.casts[actualType.cname]
                        if (castExprId == null) {
                          tryCoerceToReality()
                        } else {
                          val castExprGraph = buildGraph.exprGraphs[castExprId]
                          require(
                            task,
                            BuildTask.ExprEval(origin, castExprId, castExprGraph.mainNode, value)
                          ) { castResult, _ ->
                            coerce(
                              task,
                              origin,
                              castResult as BibixValue,
                              type,
                              dclassOrigin
                            ) { finalValue ->
                              if (finalValue == null) {
                                tryCoerceToReality()
                              } else {
                                onFinished(finalValue)
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                  else -> coerce(task, origin, value, actualType.reality, dclassOrigin) { coerced ->
                    if (coerced == null) {
                      onFinished(null)
                    } else {
                      onFinished(ClassInstanceValue(actualType.cname, coerced))
                    }
                  }
                }
              }
              is CNameValue.EnumType -> when (value) {
                is EnumValue -> {
                  if (actualType.cname != value.enumTypeName) {
                    onFinished(null)
                  } else {
                    onFinished(value)
                  }
                }
                is StringValue -> {
                  if (!actualType.values.contains(value.value)) {
                    onFinished(null)
                  } else {
                    onFinished(EnumValue(actualType.cname, value.value))
                  }
                }
                else -> onFinished(null)
              }
              else -> onFinished(null)
            }
          }
        }
        is ListType -> {
          fun rec(
            values: List<BibixValue>, elemType: BibixType, idx: Int, cc: MutableList<BibixValue>
          ) {
            if (idx == values.size) {
              check(cc.size == values.size)
              onFinished(ListValue(cc))
            } else {
              coerce(task, origin, values[idx], elemType, dclassOrigin) { elemValue ->
                if (elemValue == null) {
                  onFinished(null)
                } else {
                  cc.add(elemValue)
                  rec(values, elemType, idx + 1, cc)
                }
              }
            }
          }
          when (value) {
            is ListValue -> rec(value.values, type.elemType, 0, mutableListOf())
            is SetValue -> rec(value.values, type.elemType, 0, mutableListOf())
            else -> throw markTaskFailed(
              task,
              IllegalStateException("Cannot coerce $value to $type")
            )
          }
        }
        is SetType -> {
          fun rec(
            values: List<BibixValue>, elemType: BibixType, idx: Int, cc: MutableList<BibixValue>
          ) {
            if (idx == values.size) {
              check(cc.size == values.size)
              onFinished(SetValue(cc))
            } else {
              coerce(task, origin, values[idx], elemType, dclassOrigin) { elemValue ->
                if (elemValue == null) {
                  onFinished(null)
                } else {
                  cc.add(elemValue)
                  rec(values, elemType, idx + 1, cc)
                }
              }
            }
          }
          when (value) {
            is ListValue -> rec(value.values, type.elemType, 0, mutableListOf())
            is SetValue -> rec(value.values, type.elemType, 0, mutableListOf())
            else -> onFinished(null)
          }
        }
        is TupleType -> {
          fun rec(
            values: List<BibixValue>,
            elemTypes: List<BibixType>,
            idx: Int,
            cc: MutableList<BibixValue>
          ) {
            if (idx == values.size) {
              check(cc.size == values.size)
              onFinished(TupleValue(cc))
            } else {
              coerce(task, origin, values[idx], elemTypes[idx], dclassOrigin) { elemValue ->
                if (elemValue == null) {
                  onFinished(null)
                } else {
                  cc.add(elemValue)
                  rec(values, elemTypes, idx + 1, cc)
                }
              }
            }
          }
          when (value) {
            is TupleValue -> {
              check(type.elemTypes.size == value.values.size)
              rec(value.values, type.elemTypes, 0, mutableListOf())
            }
            is NamedTupleValue -> {
              check(type.elemTypes.size == value.values.size)
              rec(value.values.map { it.second }, type.elemTypes, 0, mutableListOf())
            }
            else -> {
              if (type.elemTypes.size == 1) {
                // 길이가 1인 tuple이면 그냥 맞춰서 반환해주기
                coerce(task, origin, value, type.elemTypes[0], dclassOrigin) { coerced ->
                  if (coerced == null) {
                    onFinished(null)
                  } else {
                    onFinished(TupleValue(coerced))
                  }
                }
              } else {
                onFinished(null)
              }
            }
          }
        }
        is NamedTupleType -> {
          when (value) {
            is NamedTupleValue -> {
              check(type.elemTypes.size == value.values.size)
              check(type.elemTypes.map { it.first } == value.values.map { it.first })
              fun rec(idx: Int, cc: MutableList<Pair<String, BibixValue>>) {
                if (idx == value.values.size) {
                  check(cc.size == value.values.size)
                  onFinished(NamedTupleValue(cc))
                } else {
                  val fieldName = value.values[idx].first
                  coerce(
                    task,
                    origin,
                    value.values[idx].second,
                    type.elemTypes[idx].second,
                    dclassOrigin
                  ) { elemValue ->
                    if (elemValue == null) {
                      onFinished(null)
                    } else {
                      cc.add(fieldName to elemValue)
                      rec(idx + 1, cc)
                    }
                  }
                }
              }
              rec(0, mutableListOf())
            }
            is TupleValue -> {
              check(type.elemTypes.size == value.values.size)
              fun rec(idx: Int, cc: MutableList<Pair<String, BibixValue>>) {
                if (idx == value.values.size) {
                  check(cc.size == value.values.size)
                  onFinished(NamedTupleValue(cc))
                } else {
                  val fieldName = type.elemTypes[idx].first
                  coerce(
                    task,
                    origin,
                    value.values[idx],
                    type.elemTypes[idx].second,
                    dclassOrigin
                  ) { elemValue ->
                    if (elemValue == null) {
                      onFinished(null)
                    } else {
                      cc.add(fieldName to elemValue)
                      rec(idx + 1, cc)
                    }
                  }
                }
              }
              rec(0, mutableListOf())
            }
            else -> {
              if (type.elemTypes.size == 1) {
                // 길이가 1인 named tuple이면 그냥 맞춰서 반환해주기
                coerce(task, origin, value, type.elemTypes[0].second, dclassOrigin) { coerced ->
                  if (coerced == null) {
                    onFinished(null)
                  } else {
                    onFinished(NamedTupleValue(type.elemTypes[0].first to coerced))
                  }
                }
              } else {
                onFinished(null)
              }
            }
          }
        }
        is UnionType -> {
          fun rec(idx: Int) {
            if (idx == type.types.size) {
              onFinished(null)
            } else {
              coerce(task, origin, value, type.types[idx], dclassOrigin) { coerced ->
                if (coerced == null) {
                  rec(idx + 1)
                } else {
                  onFinished(coerced)
                }
              }
            }
          }
          rec(0)
        }
      }
    }
  }
}
