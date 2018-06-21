package org.jetbrains.neokotlin

import org.eclipse.jgit.lib.Ref
import java.text.SimpleDateFormat
import java.util.*

class Person(val name: String, val email: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Person
        return email == other.email
    }

    override fun hashCode() = email.hashCode()
    override fun toString() = "$name <$email>"
}

class Branch(val ref: Ref, val name: String, val updatedAt: Date) {
    val isObsolete: Boolean
        get() = updatedAt < OBSOLETE_TIME && !name.endsWith("_save")

    override fun toString(): String {
        return "$name ${if (isObsolete) "[obsolete] " else ""}(" +
                "updated at ${SimpleDateFormat.getDateInstance().format(updatedAt)})"
    }
}

private val OBSOLETE_TIME = GregorianCalendar().run {
    time = Date()
    add(Calendar.MONTH, -3)
    time
}