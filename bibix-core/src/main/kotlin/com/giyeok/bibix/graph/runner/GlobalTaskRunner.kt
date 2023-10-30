package com.giyeok.bibix.graph.runner

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.ast.BibixParser
import com.giyeok.bibix.base.*
import com.giyeok.bibix.buildRuleData
import com.giyeok.bibix.graph.*
import com.giyeok.bibix.plugins.PluginInstanceProvider
import com.giyeok.bibix.plugins.PreloadedPlugin
import com.giyeok.bibix.plugins.jvm.ClassPkg
import com.giyeok.bibix.repo.BibixRepo
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import kotlinx.coroutines.sync.Mutex
import org.codehaus.plexus.classworlds.ClassWorld
import java.nio.file.Path
import kotlin.io.path.absolute

class GlobalTaskRunner private constructor(
  val globalGraph: GlobalTaskGraph,
  val preloadedPluginIds: BiMap<String, Int>,
  val preloadedPluginInstanceProviders: Map<Int, PluginInstanceProvider>,
  val preludeNames: Set<String>,
  val importInstances: MutableMap<Int, MutableList<GlobalTaskId>>,
  val buildEnv: BuildEnv,
  val repo: BibixRepo,
  val classPkgRunner: ClassPkgRunner,
) {
  companion object {
    suspend fun create(
      mainProjectLocation: BibixProjectLocation,
      preludePlugin: PreloadedPlugin,
      preloadedPlugins: Map<String, PreloadedPlugin>,
      buildEnv: BuildEnv,
      repo: BibixRepo,
      classWorld: ClassWorld,
    ): GlobalTaskRunner {
      val preludeNames = NameLookupTable.fromDefs(preludePlugin.defs).names.keys

      val mainScriptSource = mainProjectLocation.readScript()
      val mainScript = BibixParser.parse(mainScriptSource)
      val mainGraph = TaskGraph.fromScript(mainScript, preloadedPlugins.keys, preludeNames)

      val globalGraph = GlobalTaskGraph(
        mapOf(
          1 to GlobalTaskGraph.ProjectInfo(mainProjectLocation, mainScriptSource, mainGraph)
        )
      )

      val preloadedPluginInstanceProviders = mutableMapOf<Int, PluginInstanceProvider>()

      globalGraph.addProject(
        2,
        TaskGraph.fromDefs(
          packageName = preludePlugin.packageName,
          defs = preludePlugin.defs,
          preloadedPluginNames = preloadedPlugins.keys,
          preludeNames = preludeNames,
          nativeAllowed = true
        ),
        preludePlugin.script
      )
      preloadedPluginInstanceProviders[2] = preludePlugin.pluginInstanceProvider

      var preloadedPluginIdCounter = 3
      val preloadedPluginIds = mutableMapOf<String, Int>()
      preloadedPlugins.forEach { (name, plugin) ->
        preloadedPluginIds[name] = preloadedPluginIdCounter
        globalGraph.addProject(
          preloadedPluginIdCounter,
          TaskGraph.fromDefs(
            packageName = plugin.packageName,
            defs = plugin.defs,
            preloadedPluginNames = preloadedPlugins.keys,
            preludeNames = preludeNames,
            nativeAllowed = true
          ),
          plugin.script
        )
        preloadedPluginInstanceProviders[preloadedPluginIdCounter] = plugin.pluginInstanceProvider
        preloadedPluginIdCounter += 1
      }

      // TODO prelude와 preloaded plugin간의 연결

      return GlobalTaskRunner(
        globalGraph = globalGraph,
        preloadedPluginIds = HashBiMap.create(preloadedPluginIds),
        preloadedPluginInstanceProviders = preloadedPluginInstanceProviders,
        preludeNames = preludeNames,
        importInstances = mutableMapOf(),
        buildEnv = buildEnv,
        repo = repo,
        classPkgRunner = ClassPkgRunner(classWorld)
      )
    }
  }

  val mainProjectGraph get() = globalGraph.getProjectGraph(MainProjectId.projectId)

  suspend fun addProject(projectLocation: BibixProjectLocation): Int {
    val newProjectId = (globalGraph.projectGraphs.keys.maxOrNull() ?: 0) + 1
    val scriptSource = projectLocation.readScript()
    val graph =
      TaskGraph.fromScript(BibixParser.parse(scriptSource), preloadedPluginIds.keys, preludeNames)
    globalGraph.addProject(newProjectId, projectLocation, graph, scriptSource)
    return newProjectId
  }

  fun getMainProjectTaskId(taskNameTokens: List<String>): GlobalTaskId {
    when (val lookupResult = mainProjectGraph.nameLookupTable.lookupName(taskNameTokens)) {
      is NameEntryFound -> {
        when (val entry = lookupResult.entry) {
          is TargetNameEntry -> return GlobalTaskId(MainProjectId, entry.id)
          is ActionNameEntry -> TODO()
          is ActionRuleNameEntry -> TODO()
          is BuildRuleNameEntry -> TODO()
          is DataClassNameEntry -> TODO()
          is SuperClassNameEntry -> TODO()
          is EnumNameEntry -> TODO()
          is ImportNameEntry -> TODO()
          is VarNameEntry -> TODO()
        }
      }

      else -> throw IllegalStateException("Cannot find task: ${taskNameTokens.joinToString(".")}")
    }
  }

  fun getMainProjectTaskId(taskName: String): GlobalTaskId =
    getMainProjectTaskId(taskName.split('.'))

  fun getImportedProject(importNodeId: GlobalTaskId) {
    val importNode = globalGraph.getNode(importNodeId)
    check(importNode is ImportNode)
    println(importNode)
    importInstances
  }

  private val resultsMutex = Mutex()
  val results = mutableMapOf<GlobalTaskId, NodeResult>()

  private fun saveResult(
    globalTaskId: GlobalTaskId,
    result: NodeResult
  ): TaskRunResult.ImmediateResult {
    results[globalTaskId] = result
    return TaskRunResult.ImmediateResult(result)
  }

  fun saveResult(
    prjInstanceId: ProjectInstanceId,
    taskId: TaskId,
    result: NodeResult
  ): TaskRunResult.ImmediateResult =
    saveResult(GlobalTaskId(prjInstanceId, taskId), result)

  fun getResult(prjInstanceId: ProjectInstanceId, taskId: TaskId): NodeResult? =
    results[GlobalTaskId(prjInstanceId, taskId)]

  fun globalResultOrPrerequisite(
    prjInstanceId: ProjectInstanceId,
    localTaskId: TaskId,
    edgeType: TaskEdgeType
  ): TaskRunResult {
    val nodeResult = getResult(prjInstanceId, localTaskId)
    return if (nodeResult != null) {
      TaskRunResult.ImmediateResult(nodeResult)
    } else {
      TaskRunResult.UnfulfilledPrerequisites(
        listOf(GlobalTaskId(prjInstanceId, localTaskId) to edgeType)
      )
    }
  }

  fun globalWithResult(
    prjInstanceId: ProjectInstanceId,
    localTaskId: TaskId,
    edgeType: TaskEdgeType,
    withResult: (NodeResult) -> TaskRunResult
  ): TaskRunResult {
    val nodeResult = getResult(prjInstanceId, localTaskId)
    return if (nodeResult != null) {
      withResult(nodeResult)
    } else {
      TaskRunResult.UnfulfilledPrerequisites(
        listOf(GlobalTaskId(prjInstanceId, localTaskId) to edgeType)
      )
    }
  }

  fun globalWithResults(
    prjInstanceId: ProjectInstanceId,
    localTaskIds: Set<TaskId>,
    edgeType: TaskEdgeType,
    withResult: (Map<TaskId, NodeResult>) -> TaskRunResult
  ): TaskRunResult {
    val nodeResults = localTaskIds.associate { it to getResult(prjInstanceId, it) }
    return if (nodeResults.all { it.value != null }) {
      withResult(nodeResults.mapValues { (_, value) -> value!! })
    } else {
      val missingTasks = nodeResults.filter { it.value == null }.keys
      TaskRunResult.UnfulfilledPrerequisites(
        missingTasks.map { GlobalTaskId(prjInstanceId, it) to edgeType }
      )
    }
  }

  fun runTask(taskId: GlobalTaskId): TaskRunResult {
    check(taskId !in results)

    val prjInstanceId = taskId.projectInstanceId

    fun resultOrPrerequisite(localTaskId: TaskId, edgeType: TaskEdgeType): TaskRunResult =
      globalResultOrPrerequisite(prjInstanceId, localTaskId, edgeType)

    fun withResult(
      localTaskId: TaskId,
      edgeType: TaskEdgeType,
      withResult: (NodeResult) -> TaskRunResult
    ): TaskRunResult = globalWithResult(prjInstanceId, localTaskId, edgeType, withResult)

    val result: TaskRunResult = when (val node = globalGraph.getNode(taskId)) {
      is ExprNode<*> -> evaluateExprNode(prjInstanceId, node)

      is ImportNode -> {
        when (val importSourceNode = globalGraph.getNode(prjInstanceId, node.importSource)) {
          is PreloadedPluginNode -> {
            println(importSourceNode)
            val importedProjectId = preloadedPluginIds.getValue(importSourceNode.name)
            TaskRunResult.ImmediateResult(
              NodeResult.ImportResult(importedProjectId, node.importName)
            )
          }

          is ExprNode<*> -> {
            // from bibix.plugins import ktjvm  -> 지금은 import bibix.plugins as ktjvm 처럼 되고 있는듯
            withResult(node.importSource, TaskEdgeType.ValueDependency) { importSourceResult ->
              check(importSourceResult is NodeResult.ValueResult)
              val importSource = importSourceResult.value

              val projectRoot: Path
              val scriptName: StringValue?

              when (importSource) {
                is ClassInstanceValue -> {
                  check(importSource.packageName == "com.giyeok.bibix.prelude")
                  check(importSource.className == "BibixProject")

                  projectRoot =
                    (importSource.fieldValues.getValue("projectRoot") as DirectoryValue).directory
                  scriptName = importSource.fieldValues["scriptName"] as? StringValue
                }

                is StringValue -> {
                  val location = globalGraph.projectLocations.getValue(prjInstanceId.projectId)
                  projectRoot = location.projectRoot.resolve(importSource.value)
                  scriptName = null
                }

                else -> TODO()
              }

              val projectLocation = if (scriptName == null) {
                BibixProjectLocation(projectRoot.absolute())
              } else {
                BibixProjectLocation(projectRoot.absolute(), scriptName.value)
              }

              val projectId = globalGraph.getProjectIdByLocation(projectLocation)
              if (projectId == null) {
                TaskRunResult.BibixProjectImportRequired(projectLocation) { importedProjectId ->
                  saveResult(
                    prjInstanceId,
                    taskId.taskId,
                    NodeResult.ImportResult(importedProjectId, node.importName)
                  )
                }
              } else {
                TaskRunResult.ImmediateResult(NodeResult.ImportResult(projectId, node.importName))
              }
            }
          }

          else -> throw IllegalStateException()
        }
      }

      is PreloadedPluginNode -> {
        val preloadedPluginProjectId = preloadedPluginIds.getValue(node.name)
        // ImportedProjectId(preloadedPluginProjectId, taskId)
        println("$node $preloadedPluginProjectId")
        // importInstances.getOrPut(preloadedPluginProjectId) { mutableListOf() }.add(taskId)
        // 여기서는 특별히 할 일이 없는 것 같은데? projectId나 넣고 끝인듯?
        // TODO importName에 null을 주면 되나..?
        TaskRunResult.ImmediateResult(NodeResult.ImportResult(preloadedPluginProjectId, null))
      }

      is ImportInstanceNode -> {
        val importResult = getResult(prjInstanceId, node.importNode)
        check(importResult is NodeResult.ImportResult)
        importInstances.getOrPut(importResult.projectId) { mutableListOf() }.add(taskId)
        TaskRunResult.ImmediateResult(
          NodeResult.ImportInstanceResult(
            ImportedProjectId(importResult.projectId, taskId),
            node.varRedefs.mapValues { (_, varValueTaskId) ->
              GlobalTaskId(prjInstanceId, varValueTaskId)
            },
            importResult.importName
          )
        )
      }

      is MemberAccessNode -> {
        if (node.remainingNames.isEmpty()) {
          return resultOrPrerequisite(node.target, TaskEdgeType.ValueDependency)
        }
        when (val target = getResult(prjInstanceId, node.target)) {
          is NodeResult.ImportInstanceResult -> {
            val importedGraph = globalGraph.getProjectGraph(target.projectId)
            val importingName = (target.importName ?: listOf()) + node.remainingNames
            when (val nameLookupResult = importedGraph.nameLookupTable.lookupName(importingName)) {
              is NameEntryFound -> {
                globalResultOrPrerequisite(
                  target.prjInstanceId,
                  nameLookupResult.entry.id,
                  TaskEdgeType.ValueDependency
                )
              }

              is NameInImport -> {
                globalWithResult(
                  target.prjInstanceId,
                  nameLookupResult.importEntry.id,
                  TaskEdgeType.ValueDependency
                ) { importing ->
                  println(nameLookupResult)
                  println(importing)
                  check(importing is NodeResult.ImportResult)
                  val graph = globalGraph.getProjectGraph(importing.projectId)
                  val lookupResult = graph.nameLookupTable.lookupName(
                    (importing.importName ?: listOf()) + node.remainingNames
                  )
                  // TODO 이 아래 코드 확인
                  //   - lookupResult가 NameEntryFound가 아닐 수 있음
                  //   - globalResultOrPrerequisite에 주는 prjInstnaceId가 맞는지
                  check(lookupResult is NameEntryFound)
                  println("$taskId $node")
                  globalResultOrPrerequisite(
                    ImportedProjectId(
                      importing.projectId,
                      GlobalTaskId(prjInstanceId, node.target)
                    ),
                    lookupResult.entry.id,
                    TaskEdgeType.ValueDependency
                  )
                }
              }

              else -> {
                println(nameLookupResult)
                TODO()
              }
            }
          }

          is NodeResult.ValueResult -> TODO()

          else -> TODO()
        }
      }

      is BibixTypeNode -> {
        TaskRunResult.ImmediateResult(NodeResult.TypeResult(prjInstanceId, node.bibixType))
      }

      is BuildRuleNode -> {
        val impl = getResult(prjInstanceId, node.implTarget)
        val unfulfilledParamTypes = node.paramTypes.mapNotNull { (_, paramTypeNode) ->
          val paramTypeResult = getResult(prjInstanceId, paramTypeNode)
          if (paramTypeResult == null) paramTypeNode else null
        }.toSet()
        val returnTypeResult = getResult(prjInstanceId, node.returnType)
        val unfulfilledTypes =
          if (returnTypeResult == null) unfulfilledParamTypes + node.returnType else unfulfilledParamTypes
        // TODO param types 중에 unfulfill 된게 있으면 그것도 UnfulfilledPrerequisites로 반환
        if (impl == null || unfulfilledTypes.isNotEmpty()) {
          val typeDeps = unfulfilledTypes.map {
            GlobalTaskId(prjInstanceId, it) to TaskEdgeType.TypeDependency
          }
          TaskRunResult.UnfulfilledPrerequisites(
            listOf(GlobalTaskId(prjInstanceId, node.implTarget) to TaskEdgeType.ValueDependency) +
              typeDeps
          )
        } else {
          val params = node.paramTypes.map { (paramName, paramTypeNode) ->
            val paramTypeResult = getResult(prjInstanceId, paramTypeNode)!!
            check(paramTypeResult is NodeResult.TypeResult)
            paramName to paramTypeResult.type
          }
          check(returnTypeResult is NodeResult.TypeResult)
          val buildRuleData = buildRuleData {
            this.buildRuleSourceId = sourceIdOf(prjInstanceId)
            // TODO RunnableResult에 build_rule, buildRuleClassName,buildRuleMethodName 정보가 들어가야 될 듯
          }

          fun buildRuleResult(implInstance: Any): NodeResult.BuildRuleResult {
            val implMethod = try {
              implInstance::class.java.getMethod(
                node.implMethodName ?: "build",
                BuildContext::class.java
              )
            } catch (e: NoSuchMethodException) {
              throw IllegalStateException("No such method", e)
            }
            check(implMethod.trySetAccessible()) { "Method is not accessible" }

            return NodeResult.BuildRuleResult(
              prjInstanceId = prjInstanceId,
              buildRuleNode = node,
              params = params,
              returnType = returnTypeResult.type,
              returnTypePrjInstanceId = returnTypeResult.prjInstanceId,
              implInstance = implInstance,
              implMethod = implMethod,
              buildRuleData = buildRuleData,
            )
          }

          when (impl) {
            is NodeResult.PluginInstanceProviderResult -> {
              val implInstance = impl.provider.getInstance(node.implClassName)
              TaskRunResult.ImmediateResult(buildRuleResult(implInstance))
            }

            is NodeResult.ValueResult -> {
              check(impl.value is ClassInstanceValue)
              withCoercedValue(
                value = impl.value,
                type = DataClassType("com.giyeok.bibix.plugins.jvm", "ClassPkg"),
                currentPrjInstanceId = prjInstanceId,
              ) { classPkg ->
                check(classPkg is ClassInstanceValue)
                check(classPkg.packageName == "com.giyeok.bibix.plugins.jvm")
                check(classPkg.className == "ClassPkg")
                TaskRunResult.LongRunningResult {
                  val implInstance = classPkgRunner.getPluginImplInstance(
                    ClassPkg.fromBibix(classPkg),
                    node.implClassName
                  )
                  saveResult(taskId, buildRuleResult(implInstance))
                }
              }
            }

            else -> throw IllegalStateException("Cannot call $impl")
          }
        }
      }

      is ClassElemCastNode -> {
        TODO()
      }

      is DataClassTypeNode -> {
        val pkg = globalGraph.projectGraphs.getValue(prjInstanceId.projectId).packageName
        checkNotNull(pkg) { "Package name is not set" }
        // TODO TypeResult 대신 DataClassTypeResult 같은걸 넣어서 data class 자체에 대한 정보를 추가
        // TODO fieldTypes는 모두 확실히 하고 넘어가기

        val unresolvedTypes = node.fieldTypes.mapNotNull { (_, fieldTypeNode) ->
          val typeResult = getResult(prjInstanceId, fieldTypeNode)
          if (typeResult == null) fieldTypeNode else null
        }
        if (unresolvedTypes.isNotEmpty()) {
          TaskRunResult.UnfulfilledPrerequisites(unresolvedTypes.map {
            GlobalTaskId(prjInstanceId, it) to TaskEdgeType.TypeDependency
          })
        } else {
          val fieldTypes = node.fieldTypes.map { (fieldName, fieldTypeNode) ->
            val typeResult = getResult(prjInstanceId, fieldTypeNode)!!
            check(typeResult is NodeResult.TypeResult)
            fieldName to typeResult.type
          }
          // TODO defaultValues에 CallExprParamCoercionNode 추가
          // TODO node.elems 추가. elems는 안 쓸 수도 있으니 그냥 task id 상태로 넘기기
          TaskRunResult.ImmediateResult(
            NodeResult.DataClassTypeResult(
              prjInstanceId = prjInstanceId,
              packageName = pkg,
              className = node.defNode.name,
              fields = fieldTypes,
              optionalFields = node.defNode.fields.filter { it.optional }.map { it.name }.toSet(),
              defaultValues = node.defaultValues,
              classBodyElems = node.elems
            )
          )
        }
      }

      is EnumTypeNode -> {
        val pkg = globalGraph.projectGraphs.getValue(prjInstanceId.projectId).packageName
        checkNotNull(pkg) { "Package name is not set" }
        TaskRunResult.ImmediateResult(
          NodeResult.TypeResult(prjInstanceId, EnumType(pkg, node.defNode.name))
        )
      }

      is EnumValueNode -> {
        withResult(node.enumTypeNode, TaskEdgeType.TypeDependency) { typeNodeResult ->
          check(typeNodeResult is NodeResult.TypeResult)
          val enumType = typeNodeResult.type
          check(enumType is EnumType)

          // check node.memberName is valid name for the enum type
          val enumDefinedProjectId = globalGraph.getProjectIdByPackageName(enumType.packageName)
          checkNotNull(enumDefinedProjectId) { "Cannot find the package name" }
          val nameLookupResult = globalGraph.getProjectGraph(enumDefinedProjectId).nameLookupTable
            .lookupName(listOf(enumType.enumName))
          check(nameLookupResult is NameEntryFound)
          check(nameLookupResult.entry is EnumNameEntry)
          check(node.memberName in nameLookupResult.entry.def.values)

          TaskRunResult.ImmediateResult(
            NodeResult.ValueResult(
              EnumValue(enumType.packageName, enumType.enumName, node.memberName)
            )
          )
        }
      }

      is NativeImplNode -> {
        val instanceProvider = preloadedPluginInstanceProviders.getValue(prjInstanceId.projectId)
        TaskRunResult.ImmediateResult(NodeResult.PluginInstanceProviderResult(instanceProvider))
      }

      is PreludeMemberNode -> {
        TODO()
      }

      is PreludeTaskNode -> {
        val preludeGraph = globalGraph.projectGraphs.getValue(PreludeProjectId.projectId)
        when (val lookupResult = preludeGraph.nameLookupTable.lookupName(listOf(node.name))) {
          is NameEntryFound -> {
            val entry = lookupResult.entry
            globalResultOrPrerequisite(PreludeProjectId, entry.id, TaskEdgeType.Reference)
          }

          is EnumValueFound -> TODO()
          is NameFromPrelude -> TODO()
          is NameInImport -> TODO()
          is NameNotFound -> TODO()
          is NameOfPreloadedPlugin -> TODO()
          is NamespaceFound -> TODO()
        }
      }

      is SuperClassTypeNode -> {
        TODO()
      }

      is TargetNode -> resultOrPrerequisite(node.valueNode, TaskEdgeType.Definition)

      is CollectionTypeNode -> {
        when (val collectionName = node.collectionType.name) {
          "set", "list" -> {
            check(node.typeParams.size == 1)
            val elemTypeNode = node.typeParams.first()
            withResult(elemTypeNode, TaskEdgeType.TypeDependency) { elemTypeResult ->
              check(elemTypeResult is NodeResult.TypeResult)
              val elemType = elemTypeResult.type
              val type = when (collectionName) {
                "set" -> SetType(elemType)
                "list" -> ListType(elemType)
                else -> throw AssertionError()
              }
              TaskRunResult.ImmediateResult(NodeResult.TypeResult(prjInstanceId, type))
            }
          }

          else -> throw IllegalStateException("Unknown collection type: ${node.collectionType.name}")
        }
      }

      is NamedTupleTypeNode -> {
        TODO()
      }

      is TupleTypeNode -> {
        TODO()
      }

      is TypeNameNode -> {
        resultOrPrerequisite(node.typeNode, TaskEdgeType.TypeDependency)
      }

      is UnionTypeNode -> {
        val elemTypes = node.elemTypes.map { getResult(prjInstanceId, it) }
        if (elemTypes.contains(null)) {
          TaskRunResult.UnfulfilledPrerequisites(node.elemTypes.map {
            GlobalTaskId(prjInstanceId, it) to TaskEdgeType.TypeDependency
          })
        } else {
          val types = elemTypes.map { (it as NodeResult.TypeResult).type }
          TaskRunResult.ImmediateResult(NodeResult.TypeResult(prjInstanceId, UnionType(types)))
        }
      }

      is VarNode -> {
        val typeResult = getResult(prjInstanceId, node.typeNode)!!
        check(typeResult is NodeResult.TypeResult)
        val type = typeResult.type

        val varRedefs: Map<String, GlobalTaskId> = when (prjInstanceId) {
          is ImportedProjectId -> {
            val importerProject =
              globalGraph.getProjectGraph(prjInstanceId.importer.projectInstanceId.projectId)
            val redefs = importerProject.varRedefs[prjInstanceId.importer.taskId] ?: mapOf()
            redefs.mapValues { (_, taskId) ->
              GlobalTaskId(prjInstanceId.importer.projectInstanceId, taskId)
            }
          }

          MainProjectId -> TODO()
          PreludeProjectId -> TODO()
        }
        val varValue = varRedefs[node.name]
          ?: (node.defaultValueNode?.let { GlobalTaskId(prjInstanceId, it) })
        checkNotNull(varValue) { "var value is not set" }

        // TODO coercion to `type`

        globalWithResult(
          varValue.projectInstanceId,
          varValue.taskId,
          TaskEdgeType.ValueDependency
        ) { result ->
          check(result is NodeResult.ValueResult)
          withCoercedValue(result.value, type, prjInstanceId) {
            saveResult(taskId, NodeResult.ValueResult(it))
          }
        }
      }

      is ValueCoercionNode -> {
        val value = getValResult(prjInstanceId, node.value)!!
        val type = getResult(prjInstanceId, node.type)!!
        check(type is NodeResult.TypeResult)

        withCoercedValue(value, type.type, prjInstanceId) { coerced ->
          saveResult(taskId, NodeResult.ValueResult(coerced))
        }
      }

      is CallExprParamCoercionNode -> {
        val value = getValResult(prjInstanceId, node.value)!!
        val callee = getResult(prjInstanceId, node.callee)!!

        val params: List<Pair<String, BibixType>> = when (callee) {
          is NodeResult.BuildRuleResult -> {
            // TODO prjInstanceId를 이렇게 주는게 맞을지..
            callee.params
          }

          is NodeResult.DataClassTypeResult -> {
            callee.fields
          }

          else -> TODO()
        }
        val expectedType = if (node.paramPos != null) {
          params[node.paramPos].second
        } else {
          params.find { it.first == node.paramName }!!.second
        }
        withCoercedValue(value, expectedType, prjInstanceId) { coerced ->
          saveResult(taskId, NodeResult.ValueResult(coerced))
        }
      }

      is ActionDefNode -> TODO()
      is ActionRuleDefNode -> TODO()
    }

    if (result is TaskRunResult.ImmediateResult) {
      saveResult(taskId, result.result)
    }
    return result
  }

  fun getValResult(prjInstanceId: ProjectInstanceId, taskId: TaskId): BibixValue? {
    val result = getResult(prjInstanceId, taskId)
    return if (result == null) null else {
      check(result is NodeResult.ValueResult)
      result.value
    }
  }

  fun v(value: BibixValue): TaskRunResult =
    TaskRunResult.ImmediateResult(NodeResult.ValueResult(value))

  fun evaluateExprNode(prjInstanceId: ProjectInstanceId, node: ExprNode<*>): TaskRunResult =
    when (node) {
      is NoneLiteralNode -> v(NoneValue)
      is BooleanLiteralNode -> v(BooleanValue(node.literal.value))

      is MemberAccessExprNode -> {
        globalWithResult(prjInstanceId, node.target, TaskEdgeType.ValueDependency) { target ->
          when (target) {
            is NodeResult.ValueResult -> {
              fun memberValueOf(value: BibixValue, memberNames: List<String>): BibixValue =
                if (memberNames.isEmpty()) value else {
                  check(value is ClassInstanceValue)
                  val memberValue = value.fieldValues[memberNames.first()]
                    ?: throw IllegalStateException()
                  memberValueOf(memberValue, memberNames.drop(1))
                }

              TaskRunResult.ImmediateResult(
                NodeResult.ValueResult(memberValueOf(target.value, node.memberNames))
              )
            }

            else -> TODO()
          }
        }
      }

      is StringNode -> {
        val builder = StringBuilder()
        var elemCounter = 0
        node.stringExpr.elems.forEach { elem ->
          when (elem) {
            is BibixAst.EscapeChar -> {
              // TODO escape
              builder.append("\\${elem.code}")
            }

            is BibixAst.JustChar -> builder.append(elem.chr)
            is BibixAst.ComplexExpr -> {
              val elemValue = getValResult(prjInstanceId, node.exprElems[elemCounter++])!!
              // TODO coercion
              elemValue as StringValue
            }

            is BibixAst.SimpleExpr -> {
              val elemValue = getValResult(prjInstanceId, node.exprElems[elemCounter++])
              // TODO coercion
              elemValue as StringValue
            }
          }
        }
        check(elemCounter == node.exprElems.size)
        v(StringValue(builder.toString()))
      }

      is CallExprCallNode -> evaluateCallExprCallNode(prjInstanceId, node)
      is CallExprNode -> {
        val callResult = getResult(prjInstanceId, node.callNode)!!
        check(callResult is NodeResult.ValueResult)
        val returned = callResult.value

        // TODO returned가 NClassInstanceValue인 경우 처리
        //  - returned의 field 안에 있는 NClassInstnaceValue도 처리

        when (val calleeResult = getResult(prjInstanceId, node.callee)!!) {
          is NodeResult.BuildRuleResult ->
            withCoercedValue(returned, calleeResult.returnType, prjInstanceId, ::v)

          is NodeResult.DataClassTypeResult ->
            withCoercedValue(returned, calleeResult.type, prjInstanceId, ::v)

          is NodeResult.TypeResult -> {
            withCoercedValue(returned, calleeResult.type, prjInstanceId, ::v)
          }

          else -> TODO()
        }
      }

      is CastExprNode -> TODO()

      is TupleNode -> {
        val elems = node.elemNodes.map { getValResult(prjInstanceId, it)!! }
        v(TupleValue(elems))
      }

      is NamedTupleNode -> {
        val elems = node.elemNodes.map { (name, valueNode) ->
          name to getValResult(prjInstanceId, valueNode)!!
        }
        v(NamedTupleValue(elems))
      }

      is ListExprNode -> {
        val elems = mutableListOf<BibixValue>()
        node.elems.forEach { elem ->
          val elemValue = getValResult(prjInstanceId, elem.valueNode)!!
          if (!elem.isEllipsis) {
            elems.add(elemValue)
          } else {
            when (elemValue) {
              is ListValue -> elems.addAll(elemValue.values)
              is SetValue -> elems.addAll(elemValue.values)
              else -> throw IllegalStateException()
            }
          }
        }
        v(ListValue(elems))
      }

      is MergeExprNode -> TODO()
      is NameRefNode -> {
        val value = getValResult(prjInstanceId, node.valueNode)
        v(value!!)
      }

      is ParenExprNode -> v(getValResult(prjInstanceId, node.body)!!)
      is ThisRefNode -> TODO()
    }
}
