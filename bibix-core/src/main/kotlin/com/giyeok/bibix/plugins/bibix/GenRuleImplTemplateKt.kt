package com.giyeok.bibix.plugins.bibix

import com.giyeok.bibix.base.*

class GenRuleImplTemplateKt {
  fun build(context: BuildContext): BuildRuleReturn {
    val rule = context.arguments.getValue("rule") as BuildRuleDefValue
    val types = (context.arguments.getValue("types") as SetValue).values.map {
      it as TypeValue
    }
    val classTypes = types.filterIsInstance<TypeValue.ClassTypeValue>().map { it.className }
    val implName = context.arguments.getValue("implName") as StringValue
    val implInterfaceName = context.arguments.getValue("implInterfaceName") as StringValue
    return BuildRuleReturn.getClassInfos(classTypes) { classTypeDetails ->
      TODO()
    }
  }
}
