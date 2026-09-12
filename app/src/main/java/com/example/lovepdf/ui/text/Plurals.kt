package com.example.lovepdf.ui.text

fun countOf(count: Int, singular: String, plural: String = "${singular}s"): String =
    "$count ${if (count == 1) singular else plural}"
