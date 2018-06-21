package org.jetbrains.neokotlin

class PersonMerger {
    private val cache = mutableListOf<Person>()
    private val wrongEmails = mutableSetOf<String>()

    fun getWrongEmails(): Set<String> = wrongEmails

    operator fun get(person: Person): Person {
        if (!person.email.endsWith("@jetbrains.com")) {
            wrongEmails += person.email
        }

        for (saved in cache) {
            if (person.email.toLowerCase() == saved.email.toLowerCase() || person.name == saved.name) {
                return saved
            }
        }

        cache += person
        return person
    }
}