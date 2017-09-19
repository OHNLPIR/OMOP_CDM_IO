package edu.mayo.omopindexer;

/**
 * A class that contains static references to all regular expressions used in this application
 */
public class RegexpStatements {

    /**
     * A regular expression for parsing frequency and period strings. <br>
     * Should be used with CASE_INSENSITIVE and MULTILINE flags <br>
     * Important match groups (all are optional):<br>
     * - 1: frequency count <br>
     * - 2: frequency count 2 (if range in the form "x-y times") <br>
     * - 3: special -lys e.g. once, twice, thrice
     * - 4: will always contain "every" if present: indicates that frequency should be assumed to be 1 <br>
     * - 6: period length <br>
     * - 7: period range indicator (if not set, period length is equal to match 5 + match 7)
     * - 8: period length 2 (if range in the form "x-y hours/days/etc), otherwise append to match 5 for period length" <br>
     * - 9: time period unit (e.g. hours, days, seconds, etc) <br>
     * - 10: special -lys that can be seen as a frequency even while standalone
     */
    public static String FREQPERIOD =
            // Header Numeric or numeric range or the word "every"
            "(?:(?:((?:one|two|three|four|five|six|seven|eight|nine|ten|[.0-9]+))" +
                    "(?:-((?:(?:one|two|three|four|five|six|seven|eight|nine|ten|[.0-9]+))))? times?" +
                    "|(?:(once|twice|thrice) ?-? ?)|[.0-9]|(every))" +
                    // Separator
                    "(?: a |\\/| every | per | |)" +
                    // Period length or length range
                    "((?:(?:(one|two|three|four|five|six|seven|eight|nine|ten|[.0-9]+)" +
                    "?(-)?(one|two|three|four|five|six|seven|eight|nine|ten|[.0-9]+) ?)?" +
                    "(hourly|daily|monthly|weekly|yearly|day|hour|week|month|year|second|minute|monday|tuesday|wednesday|thursday|friday|saturday|sunday|d\\b|m\\b|y\\b|s\\b|h\\b|w\\b))s?))" +
                    "|(hourly|daily|monthly|weekly|yearly)";
}
