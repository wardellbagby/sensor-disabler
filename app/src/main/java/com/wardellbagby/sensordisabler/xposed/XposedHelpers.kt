package com.wardellbagby.sensordisabler.xposed

import de.robv.android.xposed.XC_MethodHook

fun methodHook(
  before: XC_MethodHook.MethodHookParam.() -> Unit = {},
  after: XC_MethodHook.MethodHookParam.() -> Unit = {}
): XC_MethodHook {
  return object : XC_MethodHook() {
    override fun beforeHookedMethod(param: MethodHookParam) {
      param.before()
    }

    override fun afterHookedMethod(param: MethodHookParam) {
      param.after()
    }
  }
}