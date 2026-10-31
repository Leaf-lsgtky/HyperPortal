.class public final Le/k;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Lcom/google/gson/t;


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Le/k$d;,
        Le/k$c;,
        Le/k$a;,
        Le/k$b;
    }
.end annotation


# instance fields
.field public final a:Ld/d;

.field public final b:Ld/i;

.field public final c:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Lcom/google/gson/p;",
            ">;"
        }
    .end annotation
.end field


# direct methods
.method public constructor <init>(Ld/d;Ld/i;Le/d;Ljava/util/List;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Le/k;->a:Ld/d;

    iput-object p2, p0, Le/k;->b:Ld/i;

    iput-object p4, p0, Le/k;->c:Ljava/util/List;

    return-void
.end method

.method public static b(Ljava/lang/Object;Ljava/lang/reflect/AccessibleObject;)V
    .locals 1

    move-object v0, p1

    check-cast v0, Ljava/lang/reflect/Member;

    invoke-interface {v0}, Ljava/lang/reflect/Member;->getModifiers()I

    move-result v0

    invoke-static {v0}, Ljava/lang/reflect/Modifier;->isStatic(I)Z

    move-result v0

    if-eqz v0, :cond_0

    const/4 p0, 0x0

    :cond_0
    sget-object v0, Ld/o$a;->a:Ld/o$a;

    invoke-virtual {v0, p0, p1}, Ld/o$a;->a(Ljava/lang/Object;Ljava/lang/reflect/AccessibleObject;)Z

    move-result p0

    if-eqz p0, :cond_1

    return-void

    :cond_1
    const/4 p0, 0x1

    invoke-static {p1, p0}, Lg/a;->d(Ljava/lang/reflect/AccessibleObject;Z)Ljava/lang/String;

    move-result-object p0

    new-instance p1, Lcom/google/gson/i;

    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p0, " is not accessible and ReflectionAccessFilter does not permit making it accessible. Register a TypeAdapter for the declaring type, adjust the access filter or increase the visibility of the element and its declaring type."

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-direct {p1, p0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw p1
.end method


# virtual methods
.method public final a(Lcom/google/gson/d;Li/a;)Lcom/google/gson/s;
    .locals 10
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "<T:",
            "Ljava/lang/Object;",
            ">(",
            "Lcom/google/gson/d;",
            "Li/a<",
            "TT;>;)",
            "Lcom/google/gson/s<",
            "TT;>;"
        }
    .end annotation

    iget-object v6, p2, Li/a;->a:Ljava/lang/Class;

    const-class v0, Ljava/lang/Object;

    invoke-virtual {v0, v6}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z

    move-result v0

    if-nez v0, :cond_0

    const/4 p0, 0x0

    return-object p0

    :cond_0
    iget-object v0, p0, Le/k;->c:Ljava/util/List;

    invoke-static {v0, v6}, Ld/o;->a(Ljava/util/List;Ljava/lang/Class;)Lcom/google/gson/p$a;

    move-result-object v0

    sget-object v1, Lcom/google/gson/p$a;->d:Lcom/google/gson/p$a;

    if-eq v0, v1, :cond_3

    sget-object v1, Lcom/google/gson/p$a;->c:Lcom/google/gson/p$a;

    if-ne v0, v1, :cond_1

    const/4 v0, 0x1

    :goto_0
    move v7, v0

    goto :goto_1

    :cond_1
    const/4 v0, 0x0

    goto :goto_0

    :goto_1
    sget-object v0, Lg/a;->a:Lg/a$a;

    invoke-virtual {v0, v6}, Lg/a$a;->d(Ljava/lang/Class;)Z

    move-result v0

    if-eqz v0, :cond_2

    new-instance v8, Le/k$d;

    const/4 v5, 0x1

    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v3, v6

    move v4, v7

    invoke-virtual/range {v0 .. v5}, Le/k;->c(Lcom/google/gson/d;Li/a;Ljava/lang/Class;ZZ)Ljava/util/LinkedHashMap;

    move-result-object p0

    invoke-direct {v8, v6, p0, v7}, Le/k$d;-><init>(Ljava/lang/Class;Ljava/util/LinkedHashMap;Z)V

    return-object v8

    :cond_2
    iget-object v0, p0, Le/k;->a:Ld/d;

    invoke-virtual {v0, p2}, Ld/d;->b(Li/a;)Ld/n;

    move-result-object v8

    new-instance v9, Le/k$c;

    const/4 v5, 0x0

    move-object v0, p0

    move-object v1, p1

    move-object v2, p2

    move-object v3, v6

    move v4, v7

    invoke-virtual/range {v0 .. v5}, Le/k;->c(Lcom/google/gson/d;Li/a;Ljava/lang/Class;ZZ)Ljava/util/LinkedHashMap;

    move-result-object p0

    invoke-direct {v9, v8, p0}, Le/k$c;-><init>(Ld/n;Ljava/util/LinkedHashMap;)V

    return-object v9

    :cond_3
    new-instance p0, Lcom/google/gson/i;

    new-instance p1, Ljava/lang/StringBuilder;

    const-string p2, "ReflectionAccessFilter does not permit using reflection for "

    invoke-direct {p1, p2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p1, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string p2, ". Register a TypeAdapter for this type or adjust the access filter."

    invoke-virtual {p1, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw p0
.end method

.method public final c(Lcom/google/gson/d;Li/a;Ljava/lang/Class;ZZ)Ljava/util/LinkedHashMap;
    .locals 34

    move-object/from16 v0, p0

    move-object/from16 v14, p1

    move-object/from16 v15, p3

    new-instance v13, Ljava/util/LinkedHashMap;

    invoke-direct {v13}, Ljava/util/LinkedHashMap;-><init>()V

    invoke-virtual/range {p3 .. p3}, Ljava/lang/Class;->isInterface()Z

    move-result v1

    if-eqz v1, :cond_0

    return-object v13

    :cond_0
    move-object/from16 v12, p2

    move/from16 v1, p4

    move-object v11, v15

    :goto_0
    const-class v2, Ljava/lang/Object;

    if-eq v11, v2, :cond_17

    invoke-virtual {v11}, Ljava/lang/Class;->getDeclaredFields()[Ljava/lang/reflect/Field;

    move-result-object v10

    const/4 v9, 0x1

    const/4 v8, 0x0

    if-eq v11, v15, :cond_2

    array-length v2, v10

    if-lez v2, :cond_2

    iget-object v1, v0, Le/k;->c:Ljava/util/List;

    invoke-static {v1, v11}, Ld/o;->a(Ljava/util/List;Ljava/lang/Class;)Lcom/google/gson/p$a;

    move-result-object v1

    sget-object v2, Lcom/google/gson/p$a;->d:Lcom/google/gson/p$a;

    if-eq v1, v2, :cond_3

    sget-object v2, Lcom/google/gson/p$a;->c:Lcom/google/gson/p$a;

    if-ne v1, v2, :cond_1

    move v1, v9

    goto :goto_1

    :cond_1
    move v1, v8

    :cond_2
    :goto_1
    move/from16 v16, v1

    goto :goto_2

    :cond_3
    new-instance v0, Lcom/google/gson/i;

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "ReflectionAccessFilter does not permit using reflection for "

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v2, " (supertype of "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1, v15}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v2, "). Register a TypeAdapter for this type or adjust the access filter."

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-direct {v0, v1}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw v0

    :goto_2
    array-length v7, v10

    move v6, v8

    :goto_3
    if-ge v6, v7, :cond_16

    aget-object v5, v10, v6

    invoke-virtual {v0, v5, v9}, Le/k;->d(Ljava/lang/reflect/Field;Z)Z

    move-result v1

    invoke-virtual {v0, v5, v8}, Le/k;->d(Ljava/lang/reflect/Field;Z)Z

    move-result v2

    if-nez v1, :cond_4

    if-nez v2, :cond_4

    move/from16 v30, v6

    move/from16 v31, v7

    move/from16 v32, v8

    move/from16 v21, v9

    move-object/from16 v25, v10

    move-object/from16 p2, v11

    move-object/from16 v22, v12

    move-object v14, v13

    goto/16 :goto_10

    :cond_4
    const-class v3, Lc/b;

    const/16 v17, 0x0

    if-eqz p5, :cond_9

    invoke-virtual {v5}, Ljava/lang/reflect/Field;->getModifiers()I

    move-result v4

    invoke-static {v4}, Ljava/lang/reflect/Modifier;->isStatic(I)Z

    move-result v4

    if-eqz v4, :cond_5

    move/from16 v18, v8

    :goto_4
    move-object/from16 v19, v17

    goto :goto_6

    :cond_5
    sget-object v4, Lg/a;->a:Lg/a$a;

    invoke-virtual {v4, v11, v5}, Lg/a$a;->a(Ljava/lang/Class;Ljava/lang/reflect/Field;)Ljava/lang/reflect/Method;

    move-result-object v4

    if-nez v16, :cond_6

    invoke-static {v4}, Lg/a;->e(Ljava/lang/reflect/AccessibleObject;)V

    :cond_6
    invoke-virtual {v4, v3}, Ljava/lang/reflect/Method;->getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;

    move-result-object v18

    if-eqz v18, :cond_8

    invoke-virtual {v5, v3}, Ljava/lang/reflect/Field;->getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;

    move-result-object v18

    if-eqz v18, :cond_7

    goto :goto_5

    :cond_7
    invoke-static {v4, v8}, Lg/a;->d(Ljava/lang/reflect/AccessibleObject;Z)Ljava/lang/String;

    move-result-object v0

    new-instance v1, Lcom/google/gson/i;

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "@SerializedName on "

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v0, " is not supported"

    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-direct {v1, v0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw v1

    :cond_8
    :goto_5
    move/from16 v18, v2

    move-object/from16 v19, v4

    goto :goto_6

    :cond_9
    move/from16 v18, v2

    goto :goto_4

    :goto_6
    if-nez v16, :cond_a

    if-nez v19, :cond_a

    invoke-static {v5}, Lg/a;->e(Ljava/lang/reflect/AccessibleObject;)V

    :cond_a
    iget-object v2, v12, Li/a;->b:Ljava/lang/reflect/Type;

    invoke-virtual {v5}, Ljava/lang/reflect/Field;->getGenericType()Ljava/lang/reflect/Type;

    move-result-object v4

    new-instance v8, Ljava/util/HashMap;

    invoke-direct {v8}, Ljava/util/HashMap;-><init>()V

    invoke-static {v2, v11, v4, v8}, Ld/b;->f(Ljava/lang/reflect/Type;Ljava/lang/Class;Ljava/lang/reflect/Type;Ljava/util/HashMap;)Ljava/lang/reflect/Type;

    move-result-object v8

    invoke-virtual {v5, v3}, Ljava/lang/reflect/Field;->getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;

    move-result-object v2

    check-cast v2, Lc/b;

    if-nez v2, :cond_b

    invoke-virtual {v5}, Ljava/lang/reflect/Field;->getName()Ljava/lang/String;

    move-result-object v2

    invoke-static {v2}, Ljava/util/Collections;->singletonList(Ljava/lang/Object;)Ljava/util/List;

    move-result-object v2

    :goto_7
    move/from16 p4, v1

    move-object v4, v2

    goto :goto_8

    :cond_b
    invoke-interface {v2}, Lc/b;->value()Ljava/lang/String;

    move-result-object v3

    invoke-interface {v2}, Lc/b;->alternate()[Ljava/lang/String;

    move-result-object v2

    array-length v4, v2

    if-nez v4, :cond_c

    invoke-static {v3}, Ljava/util/Collections;->singletonList(Ljava/lang/Object;)Ljava/util/List;

    move-result-object v2

    goto :goto_7

    :cond_c
    new-instance v4, Ljava/util/ArrayList;

    move/from16 p4, v1

    array-length v1, v2

    add-int/2addr v1, v9

    invoke-direct {v4, v1}, Ljava/util/ArrayList;-><init>(I)V

    invoke-virtual {v4, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    invoke-static {v4, v2}, Ljava/util/Collections;->addAll(Ljava/util/Collection;[Ljava/lang/Object;)Z

    :goto_8
    invoke-interface {v4}, Ljava/util/List;->size()I

    move-result v3

    move/from16 v1, p4

    move-object/from16 v15, v17

    const/4 v2, 0x0

    :goto_9
    if-ge v2, v3, :cond_14

    invoke-interface {v4, v2}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v20

    move-object/from16 p4, v15

    move-object/from16 v15, v20

    check-cast v15, Ljava/lang/String;

    if-eqz v2, :cond_d

    const/16 v20, 0x0

    goto :goto_a

    :cond_d
    move/from16 v20, v1

    :goto_a
    new-instance v1, Li/a;

    invoke-direct {v1, v8}, Li/a;-><init>(Ljava/lang/reflect/Type;)V

    iget-object v9, v1, Li/a;->a:Ljava/lang/Class;

    if-eqz v9, :cond_e

    invoke-virtual {v9}, Ljava/lang/Class;->isPrimitive()Z

    move-result v9

    if-eqz v9, :cond_e

    const/16 v22, 0x1

    goto :goto_b

    :cond_e
    const/16 v22, 0x0

    :goto_b
    invoke-virtual {v5}, Ljava/lang/reflect/Field;->getModifiers()I

    move-result v9

    invoke-static {v9}, Ljava/lang/reflect/Modifier;->isStatic(I)Z

    move-result v23

    if-eqz v23, :cond_f

    invoke-static {v9}, Ljava/lang/reflect/Modifier;->isFinal(I)Z

    move-result v9

    if-eqz v9, :cond_f

    const/16 v23, 0x1

    goto :goto_c

    :cond_f
    const/16 v23, 0x0

    :goto_c
    const-class v9, Lc/a;

    invoke-virtual {v5, v9}, Ljava/lang/reflect/Field;->getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;

    move-result-object v9

    check-cast v9, Lc/a;

    move/from16 v24, v2

    if-eqz v9, :cond_10

    iget-object v2, v0, Le/k;->a:Ld/d;

    invoke-static {v2, v14, v1, v9}, Le/d;->b(Ld/d;Lcom/google/gson/d;Li/a;Lc/a;)Lcom/google/gson/s;

    move-result-object v2

    goto :goto_d

    :cond_10
    move-object/from16 v2, v17

    :goto_d
    if-eqz v2, :cond_11

    const/4 v9, 0x1

    goto :goto_e

    :cond_11
    const/4 v9, 0x0

    :goto_e
    if-nez v2, :cond_12

    invoke-virtual {v14, v1}, Lcom/google/gson/d;->c(Li/a;)Lcom/google/gson/s;

    move-result-object v2

    :cond_12
    move-object/from16 v25, v2

    new-instance v2, Le/j;

    move-object/from16 v26, v1

    move-object v1, v2

    move-object v0, v2

    move-object v2, v15

    move/from16 v27, v3

    move-object v3, v5

    move-object/from16 v28, v4

    move/from16 v4, v20

    move-object/from16 v29, v5

    move/from16 v5, v18

    move/from16 v30, v6

    move/from16 v6, v16

    move/from16 v31, v7

    move-object/from16 v7, v19

    move-object/from16 v33, v8

    const/16 v32, 0x0

    move v8, v9

    const/16 v21, 0x1

    move-object/from16 v9, v25

    move-object/from16 v25, v10

    move-object/from16 v10, p1

    move-object/from16 p2, v11

    move-object/from16 v11, v26

    move-object v14, v12

    move/from16 v12, v22

    move-object/from16 v22, v14

    move-object v14, v13

    move/from16 v13, v23

    invoke-direct/range {v1 .. v13}, Le/j;-><init>(Ljava/lang/String;Ljava/lang/reflect/Field;ZZZLjava/lang/reflect/Method;ZLcom/google/gson/s;Lcom/google/gson/d;Li/a;ZZ)V

    invoke-interface {v14, v15, v0}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Le/k$b;

    if-nez p4, :cond_13

    move-object v15, v0

    goto :goto_f

    :cond_13
    move-object/from16 v15, p4

    :goto_f
    add-int/lit8 v2, v24, 0x1

    move-object/from16 v0, p0

    move-object/from16 v11, p2

    move-object v13, v14

    move/from16 v1, v20

    move/from16 v9, v21

    move-object/from16 v12, v22

    move-object/from16 v10, v25

    move/from16 v3, v27

    move-object/from16 v4, v28

    move-object/from16 v5, v29

    move/from16 v6, v30

    move/from16 v7, v31

    move-object/from16 v8, v33

    move-object/from16 v14, p1

    goto/16 :goto_9

    :cond_14
    move-object/from16 v29, v5

    move/from16 v30, v6

    move/from16 v31, v7

    move/from16 v21, v9

    move-object/from16 v25, v10

    move-object/from16 p2, v11

    move-object/from16 v22, v12

    move-object v14, v13

    move-object/from16 p4, v15

    const/16 v32, 0x0

    if-nez p4, :cond_15

    :goto_10
    add-int/lit8 v6, v30, 0x1

    move-object/from16 v0, p0

    move-object/from16 v11, p2

    move-object/from16 v15, p3

    move-object v13, v14

    move/from16 v9, v21

    move-object/from16 v12, v22

    move-object/from16 v10, v25

    move/from16 v7, v31

    move/from16 v8, v32

    move-object/from16 v14, p1

    goto/16 :goto_3

    :cond_15
    new-instance v0, Ljava/lang/IllegalArgumentException;

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "Class "

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual/range {p3 .. p3}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v2, " declares multiple JSON fields named \'"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-object/from16 v15, p4

    iget-object v2, v15, Le/k$b;->a:Ljava/lang/String;

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v2, "\'; conflict is caused by fields "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object v2, v15, Le/k$b;->b:Ljava/lang/reflect/Field;

    invoke-static {v2}, Lg/a;->c(Ljava/lang/reflect/Field;)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v2, " and "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-static/range {v29 .. v29}, Lg/a;->c(Ljava/lang/reflect/Field;)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-direct {v0, v1}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    throw v0

    :cond_16
    move-object/from16 p2, v11

    move-object v0, v12

    move-object v14, v13

    iget-object v0, v0, Li/a;->b:Ljava/lang/reflect/Type;

    invoke-virtual/range {p2 .. p2}, Ljava/lang/Class;->getGenericSuperclass()Ljava/lang/reflect/Type;

    move-result-object v1

    new-instance v2, Ljava/util/HashMap;

    invoke-direct {v2}, Ljava/util/HashMap;-><init>()V

    move-object/from16 v15, p2

    invoke-static {v0, v15, v1, v2}, Ld/b;->f(Ljava/lang/reflect/Type;Ljava/lang/Class;Ljava/lang/reflect/Type;Ljava/util/HashMap;)Ljava/lang/reflect/Type;

    move-result-object v0

    new-instance v12, Li/a;

    invoke-direct {v12, v0}, Li/a;-><init>(Ljava/lang/reflect/Type;)V

    iget-object v11, v12, Li/a;->a:Ljava/lang/Class;

    move-object/from16 v0, p0

    move-object/from16 v15, p3

    move-object v13, v14

    move/from16 v1, v16

    move-object/from16 v14, p1

    goto/16 :goto_0

    :cond_17
    move-object v14, v13

    return-object v14
.end method

.method public final d(Ljava/lang/reflect/Field;Z)Z
    .locals 2

    invoke-virtual {p1}, Ljava/lang/reflect/Field;->getType()Ljava/lang/Class;

    move-result-object v0

    iget-object p0, p0, Le/k;->b:Ld/i;

    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    invoke-static {v0}, Ld/i;->c(Ljava/lang/Class;)Z

    move-result v1

    if-nez v1, :cond_7

    invoke-virtual {p0, v0, p2}, Ld/i;->b(Ljava/lang/Class;Z)Z

    move-result v0

    if-eqz v0, :cond_0

    goto :goto_1

    :cond_0
    invoke-virtual {p1}, Ljava/lang/reflect/Field;->getModifiers()I

    move-result v0

    const/16 v1, 0x88

    and-int/2addr v0, v1

    if-eqz v0, :cond_1

    goto :goto_1

    :cond_1
    invoke-virtual {p1}, Ljava/lang/reflect/Field;->isSynthetic()Z

    move-result v0

    if-eqz v0, :cond_2

    goto :goto_1

    :cond_2
    invoke-virtual {p1}, Ljava/lang/reflect/Field;->getType()Ljava/lang/Class;

    move-result-object p1

    invoke-static {p1}, Ld/i;->c(Ljava/lang/Class;)Z

    move-result p1

    if-eqz p1, :cond_3

    goto :goto_1

    :cond_3
    if-eqz p2, :cond_4

    iget-object p0, p0, Ld/i;->a:Ljava/util/List;

    goto :goto_0

    :cond_4
    iget-object p0, p0, Ld/i;->b:Ljava/util/List;

    :goto_0
    invoke-interface {p0}, Ljava/util/List;->isEmpty()Z

    move-result p1

    if-nez p1, :cond_6

    invoke-interface {p0}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :cond_5
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result p1

    if-eqz p1, :cond_6

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Lcom/google/gson/a;

    invoke-interface {p1}, Lcom/google/gson/a;->b()Z

    move-result p1

    if-eqz p1, :cond_5

    goto :goto_1

    :cond_6
    const/4 p0, 0x1

    goto :goto_2

    :cond_7
    :goto_1
    const/4 p0, 0x0

    :goto_2
    return p0
.end method
