package com.giyeok.bibix.interpreter.analysis

class BibixScriptGraph {
}

sealed class Node {
  data class NameNode(val id: Int, val name: String) : Node()
  data class CallExprNode(val id: Int, val args: Map<String, Node>) : Node()

}
