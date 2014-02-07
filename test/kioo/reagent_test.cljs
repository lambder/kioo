(ns kioo.reagent-test
  (:require [cemerick.cljs.test :as t]
            [kioo.reagent :refer [content set-attr append prepend
                               remove-attr before after do->
                               set-style remove-style add-class
                               remove-class wrap unwrap set-class
                               html html-content]]
            [reagent.core :as reagent :refer [atom]]
            [kioo.util :as util]
            [goog.dom :as gdom])
  (:require-macros [kioo.reagent :refer [component]]
                   [cemerick.cljs.test :refer [are is deftest testing]]))

;; all text get surrounded by spans in om
;; its a bit ugly but it is the expected
;; behavior
(defn inner-html [node]
  (let [html (.-innerHTML node)]
      (util/strip-attr html :data-reactid)))

(defn initial-render [comp]
  (let [comp-fn #(do comp)
        container (goog.dom/createDom "div")]
    (gdom/append (.-body js/document) container)
    (reagent/render-component [comp] container)
    container))

(defn clean-up [] (gdom/removeChildren (.-body js/document)))

(defn render-dom [comp]
  (let [res (-> comp
                (initial-render)
                (inner-html))]
    (clean-up)
    res))

(deftest render-test
  (testing "basic render test"
    (let [comp #(component "simple-div.html" {})]
      (is (= "<div id=\"tmp\">test</div>"
             (render-dom comp)))))
  (testing "content replace"
    (let [atm (atom "one")
          comp #(component "simple-div.html"  
                           {[:div] (content @atm)})
          container (initial-render comp)
          html-str1 (inner-html container)
          _ (reset! atm "two")
          html-str2 (inner-html container)]
      (is (= "<div id=\"tmp\">one</div>" html-str1))
      (is (= "<div id=\"tmp\">two</div>" html-str2))
      (clean-up)))
  (testing "sub component test"
    (let [atm (atom "one")
          comp2 #(component "simple-div.html"  
                            {[:div] (do-> (remove-attr :id)
                                          (content @atm))} )
          comp #(component "simple-div.html"  
                           {[:div] (content [comp2])})
          container (initial-render comp)
          html-str1 (inner-html container)
          _ (reset! atm "two")
          html-str2 (inner-html container)]
      (is (= "<div id=\"tmp\"><div>one</div></div>"
             html-str1))
      (is (= "<div id=\"tmp\"><div>two</div></div>"
             html-str2))
      (clean-up)))
  (testing "append test"
    (let [atm (atom "one")
          comp #(component "simple-div.html"  
                           {[:div] (append @atm)})
          container (initial-render comp)
          html-str1 (inner-html container)
          _ (reset! atm "two")
          html-str2 (inner-html container)]
      ;;note that ract wraps text nodes in span tags
      ;;this is expected to be corrected soon in react but
      ;;for now this is correct
      (is (= "<div id=\"tmp\"><span>test</span><span>one</span></div>"
             html-str1))
      (is (= "<div id=\"tmp\"><span>test</span><span>two</span></div>"
             html-str2))))
  (testing "prepend test"
    (let [comp #(component "simple-div.html"
                          {[:div] (prepend "success")})]
      (is (= "<div id=\"tmp\"><span>success</span><span>test</span></div>"
             (render-dom comp)))))
  (testing "set-attr test"
    (let [comp #(component "simple-div.html"
                          {[:div] (set-attr :id "success")})]
      (is (= "<div id=\"success\">test</div>"
             (render-dom comp)))))
  (testing "remove-attr test"
    (let [comp #(component "simple-div.html"
                          {[:div] (remove-attr :id)})]
      (is (= "<div>test</div>"
             (render-dom comp)))))
  (testing "before test"
    (let [comp #(component "simple-div.html"
                          {[:div] (before "success")})]
      (is (= "<span><span>success</span><div id=\"tmp\">test</div></span>"
             (render-dom comp)))))
  (testing "after test"
    (let [comp #(component "simple-div.html"
                          {[:div] (after "success")})]
      (is (= "<span><div id=\"tmp\">test</div><span>success</span></span>"
             (render-dom comp)))))
  (testing "add-class test"
    (let [comp #(component "class-span.html" [:span]
                          {[:#s] (add-class "suc")})]
      (is (= "<span class=\"cl cls suc\" id=\"s\">testing</span>"
             (render-dom comp)))))
  (testing "remove-class test"
    (let [comp #(component "class-span.html" [:span]
                          {[:#s] (remove-class "cl")})]
      (is (= "<span class=\" cls\" id=\"s\">testing</span>"
             (render-dom comp)))))
  (testing "set-class test"
    (let [comp #(component "class-span.html" [:span]
                          {[:#s] (set-class "cl")})]
      (is (= "<span class=\" cl\" id=\"s\">testing</span>"
             (render-dom comp)))))
  (testing "set-style test"
    (let [comp #(component "style-span.html" [:span]
                          {[:#s] (set-style :display "none")})]
      (is (= "<span style=\"color:red;display:none;\" id=\"s\">testing</span>"
             (render-dom comp)))))
  (testing "remove-style test"
    (let [comp #(component "style-span.html" [:span]
                          {[:#s] (remove-style :color)})]
      (is (= "<span id=\"s\">testing</span>"
             (render-dom comp)))))
  (testing "do-> test"
    (let [comp #(component "style-span.html" [:span]
                          {[:#s] (do->
                                  (remove-attr :id)
                                  (remove-style :color))})]
      (is (= "<span>testing</span>"
             (render-dom comp)))))
  (testing "wrap test"
    (let [comp #(component "wrap-test.html" [:span]
                          {[:#s] (wrap :div {:id "test"})})]
      (is (= "<div id=\"test\"><span id=\"s\">testing</span></div>"
             (render-dom comp)))))
  (testing "unwrap test"
    (let [comp #(component "wrap-test.html" [:div]
                          {[:div] unwrap})]
      (is (= "<span id=\"s\">testing</span>"
             (render-dom comp)))))
  (testing "html test"
    (let [comp #(component "simple-div.html"
                          {[:div] (content (html [:h1 {:class "t"}
                                                  [:span "t1"]]))})]
      (is (= "<div id=\"tmp\"><h1 class=\"t\"><span>t1</span></h1></div>"
             (render-dom comp)))))
  (testing "html-content test"
    (let [comp #(component "simple-div.html"
                          {[:div] (html-content "<h1>t1</h1><em><span>t2</span></em>")})]
      (is (= "<div id=\"tmp\"><h1>t1</h1><em><span>t2</span></em></div>"
             (render-dom comp))))))