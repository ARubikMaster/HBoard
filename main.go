package keyboard

import (
	"unicode"
)

// ProcessKey takes the decimal code and the shift status.
// code: The ASCII/Unicode decimal from Android.
// isShifted: True if the Shift key is active.
func ProcessKey(code int64, isShifted bool) string {
	// Handle special characters first
	if code == 254 {
		if isShifted {
			return "Þ" // Uppercase Thorn
		}
		return "þ" // Lowercase Thorn
	}

	// Handle standard characters
	if code > 0 {
		r := rune(code)
		if isShifted {
			// unicode.ToUpper handles standard A-Z and many accented characters
			r = unicode.ToUpper(r)
		}
		return string(r)
	}

	return ""
}
