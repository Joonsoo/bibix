package com.giyeok.bibix.interpreter.task

import com.giyeok.bibix.ast.BibixAst
import com.giyeok.bibix.base.BibixValue
import com.giyeok.bibix.base.CName
import com.giyeok.bibix.base.SourceId
import com.giyeok.bibix.interpreter.coroutine.TaskElement
import com.giyeok.bibix.interpreter.expr.Definition
import com.giyeok.bibix.interpreter.expr.NameLookupContext
import com.giyeok.bibix.repo.BibixValueWithObjectHash
import com.giyeok.bibix.repo.ObjectHash
import com.google.common.annotations.VisibleForTesting
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
import java.util.concurrent.ConcurrentHashMap

// 태스크 사이의 관계를 저장해서 싸이클을 찾아내는 클래스
class TaskRelGraph {
  @VisibleForTesting
  val downstream = mutableMapOf<Task, MutableList<Task>>()

  @VisibleForTesting
  val upstream = mutableMapOf<Task, MutableList<Task>>()

  private val depsMutex = Mutex()

  private val referredNodes = ConcurrentHashMap<Pair<SourceId, Int>, BibixAst.AstNode>()

  private suspend fun add(requester: Task, task: Task): Task {
    depsMutex.withLock {
      downstream.getOrPut(requester) { mutableListOf() }.add(task)
      upstream.getOrPut(task) { mutableListOf() }.add(requester)
    }
    return task
  }

  sealed class TaskPath {
    fun toList(): List<Task> {
      val list = mutableListOf<Task>()
      var current = this
      while (current !is Nil) {
        list.add((current as Cons).task)
        current = current.next
      }
      return list.toList()
    }

    data class Cons(val task: Task, val next: TaskPath) : TaskPath()

    object Nil : TaskPath()
  }

  private suspend fun findCycleBetween(start: Task, end: Task): List<Task>? = depsMutex.withLock {
    fun traverse(task: Task, path: TaskPath): TaskPath? {
      if (task == start) {
        return path
      }
      val outgoing = downstream[task] ?: setOf()
      outgoing.forEach { next ->
        val found = traverse(next, TaskPath.Cons(next, path))
        if (found != null) {
          return found
        }
      }
      return null
    }
//    downstream[start]?.forEach { next ->
//      val found = traverse(next, TaskPath.Cons(next, TaskPath.Cons(start, TaskPath.Nil)))
//      if (found != null) {
//        return found.toList()
//      }
//    }
    return null
  }

  suspend fun <T> withTask(
    requester: Task,
    task: Task,
    body: suspend CoroutineScope.(Task) -> T
  ): T {
    check(requester != task)

    add(requester, task)

    val cycle = findCycleBetween(requester, task)
    check(cycle == null) { "Cycle found: $cycle" }

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
    fun traverse(task: Task, path: TaskPath): TaskPath {
      val ups = upstream[task]?.toList() ?: listOf()
      return if (ups.isEmpty()) path else {
        val up = ups.first()
        traverse(up, TaskPath.Cons(up, path))
      }
    }
    return traverse(task, TaskPath.Cons(task, TaskPath.Nil)).toList()
  }

  fun resolveImportTask(sourceId: SourceId, astNode: BibixAst.AstNode): Task {
    referredNodes[Pair(sourceId, astNode.nodeId)] = astNode
    return Task.ResolveImport(sourceId, astNode.nodeId)
  }
}
