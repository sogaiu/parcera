(ns parsero.core
  (:require [instaparse.core :as instaparse]
            [clojure.string :as str]))

(def grammar
    "code: form*;

    <form>: whitespace ( literal
                        | symbol
                        | list
                        | vector
                        | map
                        | set
                        | reader-macro
                        )
            whitespace;

    whitespace = #'[,\\s]*'

    list: <'('> form* <')'> ;

    vector: <'['> form* <']'> ;

    map: map-namespace? <'{'> map-content <'}'> ;

    map-namespace: <'#'> keyword ;

    map-content: (form form)*

    set: <'#{'> form* <'}'> ;

    <literal>:
          number
        | string
        | character
        | keyword
        | comment
        | symbolic
        ;

    symbolic: #'##(Inf|-Inf|NaN)'

    number: DOUBLE | RATIO | LONG;

    character: <'\\\\'> ( SIMPLE-CHAR | UNICODE-CHAR );

    <reader-macro>:
          dispatch
        | metadata
        | deref
        | quote
        | backtick
        | unquote
        | unquote-splicing
        ;

    <dispatch>: <'#'> ( function | regex | var-quote | discard | tag)

    function: list;

    metadata: <'^'> ( map-metadata | shorthand-metadata );

    <map-metadata>: map form

    <shorthand-metadata>: ( symbol | string | keyword ) form;

    regex: string;

    var-quote: <'\\''> symbol;

    quote: <'\\''> form;

    backtick: <'`'> form;

    unquote: <'~'> form;

    unquote-splicing: <'~@'> form;

    deref: <'@'> form;

    discard: <'_'> form;

    tag: !'_' symbol form;

    string : <'\"'> #'[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*' <'\"'>;

    symbol: !SYMBOL-HEAD (VALID-CHARACTERS <'/'>)? (VALID-CHARACTERS | '/') !'/';

    <keyword>: simple-keyword | macro-keyword

    simple-keyword: <':'> !':' (VALID-CHARACTERS <'/'>)? (VALID-CHARACTERS | '/') !'/';

    macro-keyword: <'::'> !':' VALID-CHARACTERS;

    comment: <';'> #'.*';

    (* Lexers -------------------------------------------------------------- *)

    <DOUBLE>: #'[-+]?(\\d+(\\.\\d*)?([eE][-+]?\\d+)?)(M)?'

    <RATIO>: #'[-+]?(\\d+)/(\\d+)'
             !'/'; (* remove ambiguity with symbols 1/5
                     1 -> number, / -> symbol, 5 -> number *)

    <LONG>: #'[-+]?(?:(0)|([1-9]\\d*)|0[xX]([\\dA-Fa-f]+)|0([0-7]+)|([1-9]\\d?)[rR]([\\d\\w]+)|0\\d+)(N)?'
            !'.'; (* remove ambiguity with symbols 1.5
                     1 -> number, . -> symbol, 5 -> number *)

    <UNICODE-CHAR>: #'u[\\dD-Fd-f]{4}';

    <SIMPLE-CHAR>:
          'newline'
        | 'return'
        | 'space'
        | 'tab'
        | 'formfeed'
        | 'backspace'
        | #'.';

    (* fragments *)
    (*
    ;; symbols cannot start with number, :, #
    ;; / is a valid symbol as long as it is not part of the name
    ;; note: added ' as invalid first character due to ambiguity in #'hello
    ;; -> [:tag [:symbol 'hello]]
    ;; -> [:var-quote [:symbol hello]]
    *)
    SYMBOL-HEAD: number | ':' | '#' | '\\''

    (*
    ;; NOTE: several characters are not allowed according to clojure reference.
    ;; https://clojure.org/reference/reader#_symbols
    ;; EDN reader says otherwise https://github.com/edn-format/edn#symbols
    ;; nil, true, false are actually symbols with special meaning ... not grammar rules
    ;; on their own
    *)
    <VALID-CHARACTERS>: #'[\\w.*+\\-!?$%&=<>\\':#]+'")

(def clojure (instaparse/parser grammar))

#_(data/diff (first (instaparse/parses clojure (slurp "./src/parsero/core.clj")))
             (second (instaparse/parses clojure (slurp "./src/parsero/core.clj"))))

;(count (instaparse/parses clojure (slurp "./src/parsero/core.clj")))

;(time (clojure (slurp "./src/parsero/core.clj")))

;(time (clojure (slurp "./src/parsero/clojure.clj")))

;(dotimes [n 100])
;(time (clojure (slurp "./src/parsero/core.clj")))

;(time (instaparse.core/parses clojure (slurp "./resources/test_cases.clj")))

(defn code
  [ast]
  (case (first ast)
    (:code)
    (str/join "" (map code (rest ast)))

    :list
    (str "(" (str/join (map code (rest ast))) ")")

    :vector
    (str "[" (str/join (map code (rest ast))) "]")

    :map
    (str/join (map code (rest ast)))

    :map-namespace
    (str "#" (code (second ast)))

    :map-content
    (str "{" (str/join (map code (rest ast))) "}")

    :set
    (str "#{" (str/join (map code (rest ast))) "}")

    (:number :whitespace :symbolic)
    (second ast)

    :string
    (str "\"" (second ast) "\"")

    :symbol
    (str/join "/" (rest ast))

    :character
    (str "\\" (second ast))

    :simple-keyword
    (str ":" (str/join "/" (rest ast)))

    :macro-keyword
    (str "::" (second ast))

    :comment
    (str ";" (second ast))

    :metadata
    (str "^" (str/join (map code (rest ast))))

    :quote
    (str "'" (str/join (map code (rest ast))))

    :regex
    (str "#" (code (second ast)))

    :var-quote
    (str "#'" (code (second ast)))

    :discard
    (str "#_" (str/join (map code (rest ast))))

    :tag
    (str "#" (str/join (map code (rest ast))))

    :backtick
    (str "`" (code (second ast)))

    :unquote
    (str "~" (code (second ast)))

    :unquote-splicing
    (str "~@" (code (second ast)))

    :deref
    (str "@" (code (second ast)))

    :function
    (str "#" (code (second ast)))))

;(code (clojure (slurp "./src/parsero/core.clj")))
;(code (clojure (slurp "./resources/test_cases.clj")))

#_(spit "resources/output.clj"
        (code (clojure (slurp "./resources/test_cases.clj"))))

;(clojure (slurp "./resources/test_cases.clj"))
;(clojure (slurp "./resources/test_cases.clj"))
