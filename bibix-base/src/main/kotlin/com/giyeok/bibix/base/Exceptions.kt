package com.giyeok.bibix.base

import com.giyeok.bibix.base.CName

class NameNotFoundException(val requiredName: CName) : Exception("Name not found: $requiredName")
