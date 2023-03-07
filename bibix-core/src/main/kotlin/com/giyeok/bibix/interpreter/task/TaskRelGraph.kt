package com.giyeok.bibix.interpreter.task

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.BibixExecutionException
import com.giyeok.bibix.interpreter.TaskDescriptor
import com.giyeok.bibix.interpreter.coroutine.TaskElement
import com.giyeok.bibix.interpreter.expr.Definition
import com.giyeok.bibix.interpreter.expr.NameLookupContext
import com.giyeok.bibix.repo.BibixValueWithObjectHash
import com.giyeok.bibix.repo.ObjectHash
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.protobuf.ByteString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// 태스크 사이의 관계를 저장해서 싸이클을 찾아내는 클래스
class TaskRelGraph {
  @VisibleForTesting
  val tasks = HashBiMap.create<Task, Int>()

  private var taskIdCounter = 0

  @VisibleForTesting
  val downstream = mutableMapOf<Int, MutableList<Int>>()

  @VisibleForTesting
  val upstream = mutableMapOf<Int, MutableList<Int>>()

  private val depsMutex = Mutex()

  private val referredNodes = ConcurrentHashMap<Pair<SourceId, Int>, BibixAst.AstNode>()

  suspend fun clear() = depsMutex.withLock {
    tasks.clear()
    downstream.clear()
    upstream.clear()
    referredNodes.clear()
  }

  private suspend fun taskId(task: Task): Int =
    depsMutex.withLock {
      val existingId = tasks[task]
      if (existingId != null) existingId else {
        taskIdCounter += 1
        val newId = taskIdCounter
        tasks[task] = newId
        newId
      }
    }

  private suspend fun add(requesterId: Int, taskId: Int) {
    depsMutex.withLock {
      downstream.getOrPut(requesterId) { mutableListOf() }.add(taskId)
      upstream.getOrPut(taskId) { mutableListOf() }.add(requesterId)
    }
  }

  // startId에서 endId로 가는 경로가 있으면 그 경로를 반환. 없으면 null 반환
  private suspend fun reachable(startId: Int, endId: Int): List<Int>? = depsMutex.withLock {
    val visited = mutableSetOf<Int>()
    val path = LinkedList<Int>()

    fun traverse(pointer: Int): List<Int>? {
      when {
        pointer == endId -> {
          path.push(pointer)
          return path.toList()
        }

        visited.contains(pointer) -> {
          return null
        }

        else -> {
          visited.add(pointer)
          path.push(pointer)

          val outgoing = downstream[pointer]
          outgoing?.forEach { next ->
            val found = traverse(next)
            if (found != null) {
              // 이 return이 traverse 함수에 대한 거라는게 조금 특이하네
              return found
            }
          }

          val popped = path.pop()
          check(popped == pointer)
          return null
        }
      }
    }
    return traverse(startId)
  }

  suspend fun <T> withTask(
    requester: Task,
    task: Task,
    body: suspend CoroutineScope.(Task) -> T
  ): T {
    check(requester != task)

    val requesterId = taskId(requester)
    val taskId = taskId(task)

    // TODO 싸이클 체크가 너무 느린데..
    val cycle = reachable(taskId, requesterId)
    check(cycle == null) {
      throw BibixExecutionException("Cycle found", tasks.inverse(), cycle!! + requesterId)
    }

    add(requesterId, taskId)

    return withContext(currentCoroutineContext() + TaskElement(task)) { body(task) }
  }

  fun evalExprTask(
    sourceId: SourceId,
    astNode: BibixAst.AstNode,
    thisValue: BibixValue?
  ): Task.EvalExpr {
    referredNodes[Pair(sourceId, astNode.nodeId)] = astNode
    return Task.EvalExpr(sourceId, astNode.nodeId, thisValue)
  }

  fun evalTypeTask(
    sourceId: SourceId,
    astNode: BibixAst.AstNode,
  ): Task.EvalType {
    referredNodes[Pair(sourceId, astNode.nodeId)] = astNode
    return Task.EvalType(sourceId, astNode.nodeId)
  }

  fun evalNameTask(
    nameLookupContext: NameLookupContext,
    name: List<String>,
    thisValue: BibixValue?
  ): Task.EvalName {
    return Task.EvalName(nameLookupContext, name, thisValue)
  }

  fun evalDefinitionTask(
    definition: Definition,
    thisValue: BibixValue?
  ): Task.EvalDefinitionTask {
    return Task.EvalDefinitionTask(definition, thisValue)
  }

  fun evalCallExprTask(
    sourceId: SourceId,
    astNode: BibixAst.AstNode,
    thisValue: BibixValue?
  ): Task.EvalCallExpr {
    referredNodes[Pair(sourceId, astNode.nodeId)] = astNode
    return Task.EvalCallExpr(sourceId, astNode.nodeId, thisValue)
  }

  fun findVarRedefsTask(cname: CName): Task.FindVarRedefsTask {
    return Task.FindVarRedefsTask(cname)
  }

  fun lookupNameTask(nameLookupContext: NameLookupContext, name: List<String>): Task.LookupName {
    return Task.LookupName(nameLookupContext, name)
  }

  fun getReferredNodeById(sourceId: SourceId, nodeId: Int): BibixAst.AstNode? =
    referredNodes[Pair(sourceId, nodeId)]

  private val objHashMap = mutableMapOf<ByteString, ObjectHash>()
  private val objMemoMap = mutableMapOf<ByteString, MutableStateFlow<BibixValueWithObjectHash?>>()
  private val memoMutex = Mutex()

  private val outputNames = mutableMapOf<CName, ObjectHash>()

  @VisibleForTesting
  suspend fun getObjHashMap() = memoMutex.withLock { objHashMap }

  @VisibleForTesting
  suspend fun getObjMemoMap() = memoMutex.withLock { objMemoMap }

  suspend fun addOutputMemo(name: CName, objectHash: ObjectHash): Boolean =
    memoMutex.withLock {
      outputNames.putIfAbsent(name, objectHash) == null
    }

  suspend fun withMemo(
    objectHash: ObjectHash,
    body: suspend CoroutineScope.() -> BibixValue
  ): BibixValueWithObjectHash = coroutineScope {
    val targetId = objectHash.targetId.targetIdBytes
    val (newMemo, stateFlow) = memoMutex.withLock {
      val existing = objMemoMap[targetId]
      if (existing == null) {
        val newStateFlow = MutableStateFlow<BibixValueWithObjectHash?>(null)
        objMemoMap[targetId] = newStateFlow
        check(!objHashMap.containsKey(targetId))
        objHashMap[targetId] = objectHash
        Pair(true, newStateFlow)
      } else {
        // check(objHashMap[targetId] == targetId)
        Pair(false, existing)
      }
    }
    if (newMemo) {
      val result = BibixValueWithObjectHash(body(), objectHash)
      stateFlow.emit(result)
      result
    } else {
      stateFlow.filterNotNull().first()
    }
  }

  fun upstreamPathTo(task: Task): List<Task> {
    val path = LinkedList<Int>()

    fun traverse(pointer: Int): List<Int> {
      val ups = upstream[pointer]?.toList()
      return if (ups == null) path else {
        val up = ups.first()
        path.push(pointer)
        return traverse(up)
      }
    }

    val foundPath = traverse(tasks.getValue(task))
    val tasksMap = tasks.inverse()
    return foundPath.map { tasksMap.getValue(it) }
  }

  fun resolveImportTask(sourceId: SourceId, astNode: BibixAst.AstNode): Task {
    referredNodes[Pair(sourceId, astNode.nodeId)] = astNode
    return Task.ResolveImport(sourceId, astNode.nodeId)
  }
}
