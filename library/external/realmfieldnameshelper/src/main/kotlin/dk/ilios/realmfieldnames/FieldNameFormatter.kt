package dk.ilios.realmfieldnames

import java.util.Locale

/**
 * Class for encapsulating the rules for converting between the field name in the Realm model class
 * and the matching name in the "&lt;class&gt;Fields" class.
 */
class FieldNameFormatter {

    @JvmOverloads
    fun format(fieldName: String?, locale: Locale = Locale.US): String {
        if (fieldName == null || fieldName == "") {
            return ""
        }

        // Normalize word separator chars
        val normalizedFieldName: String = fieldName.replace('-', '_')

        // Iterate field name using the following rules
        // lowerCase m followed by upperCase anything is considered hungarian notation
        // lowercase char followed by uppercase char is considered camel case
        // Two uppercase chars following each other is considered non-standard camelcase
        // _ and - are treated as word separators
        val result = StringBuilder(normalizedFieldName.length)

        if (normalizedFieldName.codePointCount(0, normalizedFieldName.length) == 1) {
            result.append(normalizedFieldName)
        } else {
            var previousCodepoint: Int?
            var currentCodepoint: Int? = null
            val length = normalizedFieldName.length
            var offset = 0
            while (offset < length) {
                previousCodepoint = currentCodepoint
                currentCodepoint = normalizedFieldName.codePointAt(offset)

                if (previousCodepoint != null) {
                    if (Character.isUpperCase(currentCodepoint) &&
                            !Character.isUpperCase(previousCodepoint) &&
                            previousCodepoint === 'm'.code as Int? &&
                            result.length == 1
                    ) {
                        // Hungarian notation starting with: mX
                        result.delete(0, 1)
                        result.appendCodePoint(currentCodepoint)
                    } else if (Character.isUpperCase(currentCodepoint) && Character.isUpperCase(previousCodepoint)) {
                        // InvalidCamelCase: XXYx (should have been xxYx)
                        if (offset + Character.charCount(currentCodepoint) < normalizedFieldName.length) {
                            val nextCodePoint = normalizedFieldName.codePointAt(offset + Character.charCount(currentCodepoint))
                            if (Character.isLowerCase(nextCodePoint)) {
                                result.append("_")
                            }
                        }
                        result.appendCodePoint(currentCodepoint)
                    } else if (currentCodepoint === '-'.code as Int? || currentCodepoint === '_'.code as Int?) {
                        // Word-separator: x-x or x_x
                        result.append("_")
                    } else if (Character.isUpperCase(currentCodepoint) && !Character.isUpperCase(previousCodepoint) && Character.isLetterOrDigit(
                                    previousCodepoint
                            )) {
                        // camelCase: xX
                        result.append("_")
                        result.appendCodePoint(currentCodepoint)
                    } else {
                        // Unknown type
                        result.appendCodePoint(currentCodepoint)
                    }
                } else {
                    // Only triggered for first code point
                    result.appendCodePoint(currentCodepoint)
                }
                offset += Character.charCount(currentCodepoint)
            }
        }

        return result.toString().uppercase(locale)
    }
}
