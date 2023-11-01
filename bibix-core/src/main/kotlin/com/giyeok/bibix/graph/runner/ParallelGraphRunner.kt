package com.giyeok.bibix.graph.runner

// graph 버전에서는 다른건 다 한 스레드에서 돌고,
// LongRunning과 SuspendLongRunning만 여러 스레드에서 분산시켜서 돌린다
// 또, build task간의 관계를 그래프(정확히는 DAG) 형태로 잘 관리해서 싸이클 감지와 추후 시각화 기능에 사용한다
class ParallelGraphRunner {
}
