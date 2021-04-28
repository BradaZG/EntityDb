## EntityDB

EntityDB is a client side database and normalization engine. It provides 2 features:

-     Normalization
-     Relationship support

---

### Normalization

“Normalization is a database design technique that reduces data redundancy and eliminates undesirable bugs when making CRUD operations on DB. It is the process of organising data in a database”. That includes creating tables and relationships between tables. Entitydb ensures that only one instance of entity exists in app memory.

---

### Relationship support

In most cases data we use in our apps is related, so we need to keep track of when some instance of entity is changed and update that entity on all places where it exists.

##### INSERT-NAMED

```

(edb/insert-named! ctrl :entitydb :user :user/current {:id 1 :name "Tin" :lastname "Levacic"})
```

```
ENTITY_STATE
{:entitydb/schema {},
 :entitydb/store
 {:user
  {1
   {:id 1,
    :name "Tin",
    :lastname "Levacic",
    :entitydb/id 1,
    :entitydb/type :user}}},
 :entitydb.named/item
 {:user/current {:data {:type :user, :id 1}, :meta nil}}}
```

This will create “table” user and store entity provided in last place. If we add another entity in user table like this

```
(edb/insert-named! ctrl :entitydb :user :user/active {:id 1 :name "Tin Active" :lastname "Levacic"})
```

It will override our first entity and final result in entity state will be `{:id 1 :name "Tin Active" :lastname “Levacic”}`
This is because EntityDb ensures that only one instance of entity exists in database (normalization). Those two entities are same because they have same ID.

##### INSERT-COLLECTION

```
(edb/insert-collection! ctrl :entitydb :user :user/list
[{:id 1 :name "John" :lastname "Smith"}
{:id 2 :name "Bob" :lastname "Smith"}])
```

```
ENTITY_STATE
{:entitydb/schema {},
 :entitydb/store
 {:user
  {1
   {:id 1,
    :name "John",
    :lastname "Smith",
    :entitydb/id 1,
    :entitydb/type :user},
   2
   {:id 2,
    :name "Bob",
    :lastname "Smith",
    :entitydb/id 2,
    :entitydb/type :user}}},
 :entitydb.named/item
 {:user/current {:data {:type :user, :id 1}, :meta nil}},
 :entitydb.named/collection
 {:user/list
  {:data ({:type :user, :id 1} {:type :user, :id 2}), :meta nil}}}

```

It will create “table” user (if not exists) and store entities provided in last place in user table. If table already exists it will merge data.
This will override entity we stored before, again because normalization.

Both insert-named and insert-collection takes 3 arguments:

- STORE (:user) - name of table
- ENTITY-NAME (:user/current, :user/list) - this is keyword that allows us to retrieve the elements by the name we stored it. It’s like pointer to some entities in table.
- Data - data we store in table.

### EXAMPLE

Let's say we need to store programming languages. We have API that returns languages for web, mobile and machine learning.

```
(edb/insert-collection! ctrl :entitydb :programming-languages :web [{:id 1 :name :javascript}
                                                                    {:id 2 :name :clojurescript}])
(edb/insert-collection! ctrl :entitydb :programming-languages :mobile [{:id 3 :name :swift}])
(edb/insert-collection! ctrl :entitydb :programming-languages :machine-learning [{:id 4 :name :python}])
```

This will create `PROGRAMMING LANGUAGES` table:
![table](https://user-images.githubusercontent.com/56079123/116384038-c2274600-a817-11eb-9211-addde2ca5950.png)

It will also create another table which is `POINTERS`:
![pointers](https://user-images.githubusercontent.com/56079123/116384653-5beef300-a818-11eb-93ce-847ec1937f45.png)

And now we can easily call:
`(edb/get-collection entitydb :web)` which will return only `:web` languages.

##### REMOVE-NAMED

```
(edb/remove-named! ctrl :entitydb :user/current)
```

This one will REMOVE ONLY pointer on some data in table. Table data stays unchanged.
So basically we cannot retrieve `:user/current` by calling
`(edb/get-named entitydb :user/current)`
As you can see users table is unchanged but we don’t have pointer :user/current any more.

```
ENTITY_STATE
{:entitydb/schema {},
 :entitydb/store
 {:user
  {1
   {:id 1,
    :name "John",
    :lastname "Smith",
    :entitydb/id 1,
    :entitydb/type :user},
   2
   {:id 2,
    :name "Bob",
    :lastname "Smith",
    :entitydb/id 2,
    :entitydb/type :user}}},
 :entitydb.named/collection
 {:user/list
  {:data ({:type :user, :id 1} {:type :user, :id 2}), :meta nil}}}
```

##### REMOVE-ENTITY, REMOVE-COLLECTION

Remove entity/collection does 2 things. It will delete entity (based on id) and it will delete pointer for that entity (if exists).

```
(edb/remove-entity! ctrl :entitydb :user 1)
```

It takes name of the table and id of an entity.

```
ENTITY_STATE
{:entitydb/schema {},
 :entitydb/store
 {:user
  {2
   {:id 2,
    :name "Bob",
    :lastname "Smith",
    :entitydb/id 2,
    :entitydb/type :user}}},
 :entitydb.named/item {},
 :entitydb.named/collection
 {:user/list {:data ({:type :user, :id 2}), :meta nil}}}
```

We can see that in users table there is no more user with id 1 and in pointers list there is no more user/current ( because that pointer was pointing to user with id 1)

##### SCHEMA

Entitydb schema is a way to create relationships between tables. We can also change a way how id is calculated.
If we get data from some API like this
`{:user-id 1 :name “Tin”} ` EntityDB will search for :id keyword and if it’s not provided it will store entity with id null.
We can change that with :entitydb/id attribute.
Ex. `{:user {:entitydb/id :user-id}}`
If we extend our schema

```
:keechma.entitydb/schema
{:user {:entitydb/relations
        {:languages {:entitydb.relation/path [:languages :*]
         :entitydb.relation/type :language}}}
```

After inserting data

```
(edb/insert-collection! ctrl :entitydb :user :user/list
[{:id 1 :name "Tin" :lastname "Levacic"
:languages
    [{:id 1 :language :clojure}
    {:id 2 :language :javascript}]}])
```

We will get

```
ENTITY_STATE
:entitydb/store
 {:user
  {1
   {:id 1,
    :name "Tin",
    :lastname "Levacic",
    :entitydb/id 1,
    :entitydb/type :user,
    :language
    [{:id 1, :language :clojure} {:id 2, :language :javascript}]},
   2
   {:id 2,
    :name "Bob",
    :lastname "Smith",
    :entitydb/id 2,
    :entitydb/type :user}}}
```

User with id 1 has relation to languages collection.
If we run ` (edb/get-named entitydb :user/current)` we will get:

```
{:id 1,
 :name "Tin",
 :lastname "Levacic",
 :entitydb/id 1,
 :entitydb/type :user,
 :languages [{:type :language, :id 1} {:type :language, :id 2}]}
```

We can get user languages with include query.

#### INCLUDE QUERY

It is used to include related entities. In our example we have entity user with id 1 that has relation to languages collection.

```
(edb/get-named entitydb :user/current [(q/include :languages)])
```

```
{:id 1,
 :name "Tin",
 :lastname "Levacic",
 :entitydb/id 1,
 :entitydb/type :user,
 :language [{:id 1, :language :clojure} {:id 2, :language :javascript}]}
```

## Available Scripts

In the project directory, you can run:

### `yarn start`

Runs the app in development mode.<br>
Open [http://localhost:3000](http://localhost:3000) to view it in the browser.
The page will reload if you make edits.

Builds use [Shadow CLJS](https://github.com/thheller/shadow-cljs) for maximum compatibility with NPM libraries. You'll need a [Java SDK](https://adoptopenjdk.net/) (Version 8+, Hotspot) to use it. <br>
You can [import npm libraries](https://shadow-cljs.github.io/docs/UsersGuide.html#js-deps) using Shadow CLJS. See the [user manual](https://shadow-cljs.github.io/docs/UsersGuide.html) for more information.
