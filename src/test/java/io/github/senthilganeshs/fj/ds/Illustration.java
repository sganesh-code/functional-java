package io.github.senthilganeshs.fj.ds;

import java.util.*;
import java.util.function.Function;

public class Illustration {

    public static void main(String[] args) {
        System.out.println("Using Lift");
        List.of(1,2,3).liftA2(Integer::sum, List.of(5, 6)).forEach(System.out::println);
        System.out.println("Using Apply");
        List.of(1,2,3).apply(List.of(i -> i + 5, i -> i + 6)).forEach(System.out::println);
        System.out.println("Using Traverse");
        List.of(1,2,3).traverse(i -> List.of(i + 5, i + 6)).forEach(System.out::println);
        System.out.println("Using Sequence");
        Collection.sequence(List.of(List.of(2,3), List.of(4,5), List.of(6, 7))).forEach(System.out::println);

    }

    class RawContact {
        final String name;
        final java.util.List<String> phoneParts;
        final java.util.List<String> tags;
        final String referrerOrNull; // null if none

        RawContact(String name, java.util.List<String> phoneParts,
                   java.util.List<String> tags, String referrerOrNull) {
            this.name = name;
            this.phoneParts = phoneParts;
            this.tags = tags;
            this.referrerOrNull = referrerOrNull;
        }
    }

    static class ContactSummary {
        final String name;
        final String phone;
        final String tagsCSV;
        ContactSummary(String name, String phone, String tagsCSV) {
            this.name = name; this.phone = phone; this.tagsCSV = tagsCSV;
        }
        public String toString() { return name + " | " + phone + " | " + tagsCSV; }
    }


    class ImperativeSolution {
        static boolean isDigitsOrPlusFirst(java.util.List<String> parts) {
            if (parts.isEmpty()) return false;
            String first = parts.get(0);
            if (!first.startsWith("+") || first.length() < 2 || !first.substring(1).chars().allMatch(Character::isDigit)) return false;
            for (int i = 1; i < parts.size(); i++) {
                if (!parts.get(i).chars().allMatch(Character::isDigit)) return false;
            }
            return true;
        }

        static String formatPhone(java.util.List<String> parts) {
            return String.join("-", parts); // simple; doesn’t add plus—already in first part
        }

        static String enrichName(RawContact rc) {
            return rc.referrerOrNull == null ? rc.name : rc.name + " (ref:" + rc.referrerOrNull + ")";
        }

        static String tagsCSV(java.util.List<String> tags) {
            return String.join(",", tags);
        }

        static class Error { final String name; final String reason;
            Error(String n, String r){name=n;reason=r;}
            public String toString(){return name+": "+reason;}
        }

        static void run(java.util.List<RawContact> input) {
            java.util.Map<String, ContactSummary> dedup = new LinkedHashMap<>();
            java.util.List<Error> errors = new ArrayList<>();
            java.util.Set<String> countryCodes = new TreeSet<>(); // sorted

            for (RawContact rc : input) {
                // validation
                if (rc.tags.stream().anyMatch(t -> t.equalsIgnoreCase("spam"))) {
                    errors.add(new Error(rc.name, "contact is spam"));
                    continue;
                }
                if (!isDigitsOrPlusFirst(rc.phoneParts)) {
                    errors.add(new Error(rc.name, "invalid phone parts"));
                    continue;
                }

                // success path
                String phone = formatPhone(rc.phoneParts);
                String name = enrichName(rc);
                String tagsCSV = tagsCSV(rc.tags);

                // dedupe by name (first wins)
                dedup.putIfAbsent(name, new ContactSummary(name, phone, tagsCSV));

                // country code (first part)
                countryCodes.add(rc.phoneParts.get(0));
            }

            // sorted list of summaries by name
            java.util.List<ContactSummary> summaries = new ArrayList<>(dedup.values());
            summaries.sort(Comparator.comparing(cs -> cs.name));

            System.out.println("Summaries:");
            summaries.forEach(System.out::println);

            System.out.println("Country codes: " + countryCodes);
            System.out.println("Errors: " + errors);
        }

        class DeclarativeSolution {

            class RawContact {
                final String name;
                final List<String> phoneParts;
                final List<String> tags;
                final String referrerOrNull; // null if none

                RawContact(String name,
                           List<String> phoneParts,
                           List<String> tags,
                           String referrerOrNull) {
                    this.name = name;
                    this.phoneParts = phoneParts;
                    this.tags = tags;
                    this.referrerOrNull = referrerOrNull;
                }
            }

            static class ContactSummary {
                final String name;
                final String phone;
                final String tagsCSV;
                ContactSummary(String name, String phone, String tagsCSV) {
                    this.name = name; this.phone = phone; this.tagsCSV = tagsCSV;
                }
                public String toString() { return name + " | " + phone + " | " + tagsCSV; }
            }

            static void run(List<RawContact> input) {


            }



            private static Collection<Error> checkForPhoneParts(RawContact rc) {
                 return rc.phoneParts
                        .take(1).flatMap(first ->
                                (first.startsWith("+") && first.substring(1).chars().allMatch(Character::isDigit))
                                        ? Maybe.some(first)
                                        : Maybe.nothing()
                        ).concat(
                                rc.phoneParts.drop(1).traverse(p -> p.chars().allMatch(Character::isDigit) ? Maybe.some(p) : Maybe.nothing())
                                        .map(ignored -> "+") // dummy map to keep type; success is enough
                        ).foldl(Maybe.some(new Error(rc.name, "invalid phone parts")),
                                 (error, parts) -> Maybe.nothing());
            }

            private static Collection<Error> checkForSpam(RawContact rc) {
                return rc.tags.find(DeclarativeSolution::isSpam)
                               .map(tag -> new Error(rc.name, "contact is spam"));
            }

            private static Boolean isSpam(String t) {
                return t.equalsIgnoreCase("spam");
            }


        }

    }

}
