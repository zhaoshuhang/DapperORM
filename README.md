# 简介 #

HappyORM是OA组在工作中产出的一款轻量级快速开发Java数据库应用的ORM工具，底层基于JDBC

# 应用场景 #

* 需要快速开发一个Demo程序，又不希望去引用复杂的Spring框架和例如MyBatis等ORM框架。

# 安装方法 #

1. 引用happy.orm-1.0.jar
    1. 将happy.orm-1.0.jar拷贝至工程目录，例如${basedir}/lib
    2. 引入包。在POM.xml中加入
        ```xml
        <dependencies>
            <dependency>
                <groupId>com.sinux</groupId>
                <artifactId>happy.orm</artifactId>
                <version>1.0</version>
                <scope>system</scope>
                <systemPath>${basedir}/happy.orm-1.0.jar</systemPath>
            </dependency>
        </dependencies>
        ```
    3. 配置编译插件
        ```xml
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <version>3.1.1</version>
                    <executions>
                        <execution>
                            <id>copy-dependencies</id>
                            <phase>compile</phase>
                            <goals>
                                <goal>copy-dependencies</goal>
                            </goals>
                            <configuration>
                                <!--这个路径根据自己实际情况配-->
                                <outputDirectory>${project.build.directory}/${project.build.finalName}/WEB-INF/lib</outputDirectory>
                                <includeScope>system</includeScope>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </pluginManagement>
        ```
2. 将JDBC配置存到classPath下的db.properties。文件内容如下
    ```text
    jdbc.url=jdbc:mysql://192.168.99.99:3306/test_db?useUnicode=true&characterEncoding=utf-8
    jdbc.username=root
    jdbc.password=rootPassword
    ```
3. 将Class和数据库的表关联
    1. 在Class上加入@Table注解

        __tb_foo__ 的数据

        |id|name|bar_id|bar_name|last_change_time|
        |--|--|--|--|--|
        |1|foo1|1|bar2|235254535|

        __tb_bar__ 的数据

        |id|name|value|
        |--|--|--|
        |1|bar1|123|
        |2|bar2|456|

        ```java
        @Table("tb_foo")
        public class Foo{
        }

        @Table("tb_bar")
        public class Bar{
        }
        ```
    2. 在get方法上加上注解
        ```java
        @Table("tb_foo")
        public class Foo{
            private String name;
            private String id;
            private Date changeTime;
            private Bar bar1;
            private Bar bar2;

            @Column // 当数据库的字段名和field名一致时，可以不指定列名
            @CanInsert // 表明此字段的值在Insert时可以被插入到数据库
            @CanUpdate // 表明此字段的值在Update时可以被写入到数据库
            public String getName(){
                return name;
            }

            @Column
            @VerifyOnUpdate // 在Update时自动加上WHERE id = #{id}
            @VerifyOnDelete // 在Delete时自动加上WHERE id = #{id}
            public String getId(){
                return id;
            }

            // 当Field名和列名不一致时必须指定列名
            @Column("last_change_time")
            public getChangeTime(){
                return changeTime;
            }

            // 当Bar类中有且仅有一个ForeignKey注解,
            // 并且关联的字段就是这个有ForeignKey注解的字段时
            // 可以省略Column注解的refField/refColumnMethod属性
            @Column("bar_id")
            public Bar getBar1(){
                return bar1;
            }

            // 可以指定与Bar类中的哪个方法关联，方法必须有@Column注解
            @Column(value="bar_name", refColumnMethod="getName")
            public Bar getBar2(){
                return bar2;
            }

            // 省略所有的setter方法
        }

        @Table("tb_bar")
        public class Bar{
            private String name;
            private String id;
            private Integer value;

            @Column
            public String getName(){
                return name;
            }

            @Column
            @ForeignKey
            public String getId(){
                return value;
            }

            @Column
            public Integer getValue(){
                return value;
            }

            // 省略所有的setter方法
        }
        ```
4. 增删改查

```java
// 查询全部
Sql sql = SqlBuilder.select(Foo.class);
List<Foo> = CommonDal.execQuery(sql).toList();

// 带Where条件的查询
Sql sql = SqlBuilder.select(Foo.class).where(Foo:getName, Operator.Like, "foo%");
List<Foo> = CommonDal.execQuery(sql).toList();

// 带And，OR的复杂条件查询
List<String> barNameList = new ArrayList<>();
barNameList.add(XXX);
Sql sql = SqlBuilder.select(Foo.class).where(Foo::getName, Operator.Like, "foo%")
    .and(
        a->a.where(Foo::getId,Operator.LargerThan,100),
        a->a.or(
            b->b.where(Foo::getBar2,Relation.In, barNameList),
            b->b.where(Foo::getBar2,Operator.Equals, null)
        )
    );

// 编译后的Where：
// WHERE name LIKE 'foo%'
// AND (
//   (id>100)
//   AND (
//     (bar_name in (xxx))
//     OR
//     (bar_name is null)
//   )
// )
List<Foo> = CommonDal.execQuery(sql).toList();

// and 和 or 支持对集合构造查询语句
List<String> searchKeys = new ArrayList<>();
searchKeys.add("tom");
searchKeys.add("jerry");
Sql sql = SqlBuild.select(User.class).or(
    searchKeys,
    (key,a) -> a.or(
        b-> b.where(User::getFirstName, Operator.Like, "%" + key + "%"),
        b-> b.where(User::getLastName, Operator.Like, "%" + key + "%")
    )
)

// 编译后的查询语句为
// ((firstName LIKE '%tom%') or (lastName LIKE '%tom%') or (firstName LIKE '%jerry%') or (lastName LIKE '%jerry%'))
```

```java
Foo foo;

Sql sql = SqlBuilder.insert(foo);
CommonDal.execNonQuery(sql);

Sql sql = SqlBuilder.update(foo);
CommonDal.execNonQuery(sql);

Sql sql = SqlBuilder.delete(foo);
CommonDal.execNonQuery(sql);

```

当然，我们也为开发人员提供了最底层的方法，以应对我们无法满足的要求：

```java
// 不带参数的SQL语句
CommonDal.execNonQuery("DELETE FROM table1 WHERE name is null");

// 带参数的SQL语句
Foo foo = new Foo();
foo.setName("xxx");
CommonDal.execNonQuery("UPDATE FROM tb_foo WHERE name = #{name}",foo);
// 参数也可以是Map 或是 ObjMap
Map<String,Object> parameters = new HashMap<>();
parameters.put("nameValue","xxxx");
CommonDal.execNonQuery("UPDATE FROM tb_foo WHERE name = #{nameValue}",parameters);

// 以及允许你自己来管理Connection来连接多个数据库
Connection connection = DriverManager.getConnection("jdbc:mysql://192.168.99.99:3306/test_db?useUnicode=true&characterEncoding=utf-8", "root", "rootPassword");
Sql sql = SqlBuilder.select(Foo.class).toSql();
List<Foo> fooList = CommonDal.execQuery(connection, sql).toList();
// 同理也适用于
// CommonDal.execNonQuery(Connection connection, String sql)
// CommonDal.execNonQuery(Connection connection, String sql, Object parameters)
// CommonDal.execNonQuery(Connection connection, String sql)
// CommonDal.execNonQuery(Connection connection, String sql, Object parameters)
```