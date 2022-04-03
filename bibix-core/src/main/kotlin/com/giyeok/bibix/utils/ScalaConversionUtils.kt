package com.giyeok.bibix.utils

fun <T> scala.collection.immutable.List<T>.toKtList(): List<T> =
  List<T>(this.size()) { idx -> this.apply(idx) }

fun <T> scala.Option<T>.getOrNull(): T? =
  if (this.isEmpty) null else this.get()
