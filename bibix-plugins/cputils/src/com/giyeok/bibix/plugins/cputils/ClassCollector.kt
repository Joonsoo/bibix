package com.giyeok.bibix.plugins.cputils

import java.io.File
import java.lang.reflect.Method
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.zip.ZipInputStream
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

class ClassCollector(val classpaths: List<File>, allDeps: List<File>) {
  private val fileSystem = FileSystems.getDefault()
  private val loader =
    // allDeps가 classPaths를 포함해서 allDeps로만 해도 되긴 할텐데.. 그냥 이렇게 해둘래
    URLClassLoader((classpaths + allDeps).map { it.toURI().toURL() }.toTypedArray())
  private val allClasses = mutableSetOf<String>()
  val graph = ClassGraph()

  init {
    classpaths.forEach { cp ->
      if (cp.isDirectory) {
        loadClassesFromDirectory(cp)
      } else {
        loadClassesFromJar(cp)
      }
    }
    traverseClassRels(allClasses.toList(), mutableSetOf())
  }

  private fun addClass(classPath: String) {
    val className = classPath.substringBeforeLast('.').replace('/', '.')
    allClasses.add(className)
  }

  private fun loadClassesFromDirectory(file: File) {
    val basePath = fileSystem.getPath(file.canonicalPath)
    Files.walk(basePath).forEach { path ->
      if (path.isRegularFile() && path.extension == "class") {
        addClass(path.relativeTo(basePath).normalize().pathString)
      }
    }
  }

  private fun loadClassesFromJar(file: File) {
    file.inputStream().buffered().use { fis ->
      ZipInputStream(fis).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
          if (!entry.isDirectory && entry.name.endsWith(".class")) {
            addClass(entry.name)
          }
          entry = zis.nextEntry
        }
      }
    }
  }

  private fun URLClassLoader.tryLoadClass(className: String): Class<*>? =
    try {
      this.loadClass(className)
    } catch (e: ClassNotFoundException) {
      null
    } catch (e: NoClassDefFoundError) {
      null
    }

  private fun traverseClassRels(queue: List<String>, visited: MutableSet<String>) {
    if (queue.isEmpty()) {
      return
    }
    val className = queue.first()
    val rest = queue.drop(1)
    if (visited.contains(className)) {
      traverseClassRels(rest, visited)
    } else {
      visited.add(className)
      val cls = loader.tryLoadClass(className)
      if (cls != null) {
        val superClasses =
          listOfNotNull(cls.superclass?.canonicalName) + cls.interfaces.map { it.canonicalName }
        if (superClasses.isNotEmpty()) {
          val newSupers = superClasses.toSet() - visited
          newSupers.forEach { superClass ->
            graph.addRelation(superClass, className)
          }
          if (newSupers.isNotEmpty()) {
            traverseClassRels(rest + newSupers, visited)
            return
          }
        }
      }
      traverseClassRels(rest, visited)
    }
  }

  fun findSubclassNamesOf(className: String): Set<String> {
    val subclasses = mutableSetOf<String>()
    fun traverse(queue: List<String>): Set<String> =
      if (queue.isEmpty()) {
        subclasses
      } else {
        val head = queue.first()
        val rest = queue.drop(1)
        val newSubs = (graph.subs[head] ?: setOf()) - subclasses
        subclasses.addAll(newSubs)
        traverse(rest + newSubs)
      }
    return traverse(listOf(className)).intersect(allClasses)
  }

  fun findSubclassesOf(className: String): List<Class<*>> {
    return findSubclassNamesOf(className).mapNotNull { loader.tryLoadClass(it) }
  }

  fun findClassesWithAnnotation(annotationName: String): List<Class<*>> =
    allClasses.mapNotNull { loader.tryLoadClass(it) }.filter { cls ->
      cls.declaredAnnotations.any { annot ->
        annot.annotationClass.qualifiedName == annotationName
      }
    }

  fun findMethodsWithAnnotation(annotationName: String): List<Method> =
    allClasses.mapNotNull { loader.tryLoadClass(it) }
      .flatMap { cls -> cls.declaredMethods.asIterable() }
      .filter { method ->
        method.declaredAnnotations.any { annot ->
          annot.annotationClass.qualifiedName == annotationName
        }
      }

  fun getClass(className: String) = loader.loadClass(className)
}

class ClassGraph {
  // key의 super type들의 set
  val supers = mutableMapOf<String, MutableSet<String>>()

  // key의 sub type들의 set
  val subs = mutableMapOf<String, MutableSet<String>>()

  fun addRelation(superClass: String, subClass: String) {
    supers.putIfAbsent(subClass, mutableSetOf())
    supers.getValue(subClass).add(superClass)

    subs.putIfAbsent(superClass, mutableSetOf())
    subs.getValue(superClass).add(subClass)
  }
}
