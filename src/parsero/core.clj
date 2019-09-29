(ns parsero.core
  (:require [instaparse.core :as instaparse]
            [clojure.data :as data]))


(def grammar
    "file: forms

    whitespace = #'[,\\s]*'

    <forms>: form* ;

    <form>: <whitespace> ( literal
                          | symbol
                          | list
                          | vector
                          | map
                          | set
                          | reader_macro
                          )
            <whitespace>;

    list: <'('> forms <')'> ;

    vector: <'['> forms <']'> ;

    map: <'{'> (form form)* <'}'> ;

    set: <'#{'> forms <'}'> ;

    <literal>:
          number
        | string
        | character
        | keyword
        | <COMMENT>
        ;

    symbol: SIMPLE_SYMBOL | NAMESPACED_SYMBOL;

    keyword: SIMPLE_KEYWORD | NAMESPACED_KEYWORD | MACRO_KEYWORD;

    number: DOUBLE | RATIO | LONG;

    character:
          SIMPLE_CHAR
        | UNICODE_CHAR
        ;

    reader_macro:
          lambda
        | metadata
        | regex
        | var_quote
        | discard
        | tag
        | deref
        | quote
        | backtick
        | unquote
        | unquote_splicing
        ;

    lambda: '#(' forms ')';

    metadata: <'^'> ( map_metadata | shorthand_metadata );

    <map_metadata>: map form

    <shorthand_metadata>: ( symbol | string | keyword ) form;

    regex: <'#'> string;

    var_quote: <'#\\''> symbol;

    quote: <'\\''> form;

    backtick: <'`'> form;

    unquote: <'~'> form;

    unquote_splicing: <'~@'> form;

    deref: <'@'> form;

    discard: <'#_'> form;

    tag: <'#'> !'_' symbol form;

    string : #'\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"';

    (* Lexers -------------------------------------------------------------- *)

    <SIMPLE_SYMBOL>: !symbol-head (valid-characters | '/') !'/';

    <NAMESPACED_SYMBOL>: !symbol-head (valid-characters <'/'> valid-characters);

    <SIMPLE_KEYWORD>: <':'> !':' valid-characters !'/';

    <NAMESPACED_KEYWORD>: <':'> valid-characters <'/'> valid-characters;

    <MACRO_KEYWORD>: <'::'> valid-characters;

    <DOUBLE>: #'([-+]?[0-9]+(\\.[0-9]*)?([eE][-+]?[0-9]+)?)(M)?'

    <RATIO>: #'([-+]?[0-9]+)/([0-9]+)'

    <LONG>: #'([-+]?)(?:(0)|([1-9][0-9]*)|0[xX]([0-9A-Fa-f]+)|0([0-7]+)|([1-9][0-9]?)[rR]([0-9A-Za-z]+)|0[0-9]+)(N)?'
            !'.';

    COMMENT: #';.*';

    <UNICODE_CHAR>: #'\\\\u[0-9D-Fd-f]{4}';

    <SIMPLE_CHAR>: <'\\\\'>  ( 'newline'
                             | 'return'
                             | 'space'
                             | 'tab'
                             | 'formfeed'
                             | 'backspace'
                             | 'c'
                             | #'.' );

    (* fragments *)
    (*
    ;; symbols cannot start with number, :, #
    ;; / is a valid symbol as long as it is not part of the name
    ;; note: added ' as invalid first character due to ambiguity in #'hello
    ;; -> [:tag [:symbol hello]]
    ;; -> [:var_quote [:symbol hello]]
    *)
    symbol-head: #'[0-9:#\\']'

    (*
    ;; NOTE: several characters are not allowed according to clojure reference.
    ;; https://clojure.org/reference/reader#_symbols
    ;; EDN reader says otherwise https://github.com/edn-format/edn#symbols
    ;; nil, true, false are actually symbols with special meaning ... not grammar rules
    ;; on their own
    *)
    <valid-characters>: #'[\\w.*+\\-!?$%&=<>\\':#]+'")

(def clojure (instaparse/parser grammar))
clojure
(instaparse/parses clojure (str '(defn foo
                                   "I don't do a whole lot."
                                   [x]
                                   (println x 9.78 "Hello, World!"))))

#_(data/diff (first (instaparse/parses clojure (slurp "./src/parsero/core.clj")))
             (second (instaparse/parses clojure (slurp "./src/parsero/core.clj"))))

;(count (instaparse/parses clojure (slurp "./src/parsero/core.clj")))

;(time (clojure (slurp "./src/parsero/core.clj") :unhide :all))
;(time (clojure (slurp "./src/parsero/core.clj")))

;; TODO: is this a bug ?
#_(def foo.bar "hello")
;; TODO: is this a bug ?
#_(def . "hello")

;; TODO: is this a bug ?
#_(defn foo.bar [a.b]) ;; not valid ... why ?

;; TODO: is this a bug ? ;; a keyword starting with a number
;; :1hello.world

;; todo: https://clojure.org/reference/reader#_character

(meta ^{:a 1 :b 2} [1.2 2.3 0x12])

(meta ^String [1 2 3])

'(meta ^"String" [1 2 3])

#"hellow|wprld"

nil
true
;(time (count (instaparse/parses clojure (slurp "./src/parsero/clojure.clj"))))
#'clojure []

\-
;; todo: the following shows that we have ambigous grammar :/
(instaparse/parses clojure (str '(defn hello? [x] (if (nil? x) nil ::tap-nil x))))
(clojure (str '(defn hello? [x] (if (nil? x) ::tap-nil x))))

(clojure (str '(nil)))
(set! *print-length* 30)
