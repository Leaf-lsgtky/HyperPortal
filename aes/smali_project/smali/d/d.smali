.class public final Ld/d;
.super Ljava/lang/Object;
.source "SourceFile"


# instance fields
.field public final a:Ljava/util/Map;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/Map<",
            "Ljava/lang/reflect/Type;",
            "Lcom/google/gson/e<",
            "*>;>;"
        }
    .end annotation
.end field

.field public final b:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Lcom/google/gson/p;",
            ">;"
        }
    .end annotation
.end field


# direct methods
.method public constructor <init>(Ljava/util/Map;Ljava/util/List;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Ld/d;->a:Ljava/util/Map;

    iput-object p2, p0, Ld/d;->b:Ljava/util/List;

    return-void
.end method

.method public static a(Ljava/lang/Class;)Ljava/lang/String;
    .locals 2
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/Class<",
            "*>;)",
            "Ljava/lang/String;"
        }
    .end annotation

    invoke-virtual {p0}, Ljava/lang/Class;->getModifiers()I

    move-result v0

    invoke-static {v0}, Ljava/lang/reflect/Modifier;->isInterface(I)Z

    move-result v1

    if-eqz v1, :cond_0

    invoke-virtual {p0}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object p0

    const-string v0, "Interfaces can\'t be instantiated! Register an InstanceCreator or a TypeAdapter for this type. Interface name: "

    invoke-virtual {v0, p0}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    return-object p0

    :cond_0
    invoke-static {v0}, Ljava/lang/reflect/Modifier;->isAbstract(I)Z

    move-result v0

    if-eqz v0, :cond_1

    invoke-virtual {p0}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object p0

    const-string v0, "Abstract classes can\'t be instantiated! Register an InstanceCreator or a TypeAdapter for this type. Class name: "

    invoke-virtual {v0, p0}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p0

    return-object p0

    :cond_1
    const/4 p0, 0x0

    return-object p0
.end method


# virtual methods
.method public final b(Li/a;)Ld/n;
    .locals 9
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "<T:",
            "Ljava/lang/Object;",
            ">(",
            "Li/a<",
            "TT;>;)",
            "Ld/n<",
            "TT;>;"
        }
    .end annotation

    const/4 v0, 0x0

    const/4 v1, 0x1

    iget-object v2, p1, Li/a;->b:Ljava/lang/reflect/Type;

    iget-object v3, p0, Ld/d;->a:Ljava/util/Map;

    invoke-interface {v3, v2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Lcom/google/gson/e;

    if-eqz v4, :cond_0

    new-instance p0, Ld/d$a;

    invoke-direct {p0, v4, v2}, Ld/d$a;-><init>(Lcom/google/gson/e;Ljava/lang/reflect/Type;)V

    return-object p0

    :cond_0
    iget-object p1, p1, Li/a;->a:Ljava/lang/Class;

    invoke-interface {v3, p1}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Lcom/google/gson/e;

    if-eqz v3, :cond_1

    new-instance p0, Ld/d$b;

    invoke-direct {p0, v3, v2}, Ld/d$b;-><init>(Lcom/google/gson/e;Ljava/lang/reflect/Type;)V

    return-object p0

    :cond_1
    const-class v3, Ljava/util/EnumSet;

    invoke-virtual {v3, p1}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z

    move-result v3

    const/4 v4, 0x0

    if-eqz v3, :cond_2

    new-instance v3, Ld/e;

    invoke-direct {v3, v2, v0}, Ld/e;-><init>(Ljava/lang/reflect/Type;I)V

    goto :goto_0

    :cond_2
    const-class v3, Ljava/util/EnumMap;

    if-ne p1, v3, :cond_3

    new-instance v3, Ld/e;

    invoke-direct {v3, v2, v1}, Ld/e;-><init>(Ljava/lang/reflect/Type;I)V

    goto :goto_0

    :cond_3
    move-object v3, v4

    :goto_0
    if-eqz v3, :cond_4

    return-object v3

    :cond_4
    iget-object p0, p0, Ld/d;->b:Ljava/util/List;

    invoke-static {p0, p1}, Ld/o;->a(Ljava/util/List;Ljava/lang/Class;)Lcom/google/gson/p$a;

    move-result-object p0

    invoke-virtual {p1}, Ljava/lang/Class;->getModifiers()I

    move-result v3

    invoke-static {v3}, Ljava/lang/reflect/Modifier;->isAbstract(I)Z

    move-result v3

    sget-object v5, Lcom/google/gson/p$a;->a:Lcom/google/gson/p$a;

    if-eqz v3, :cond_5

    :catch_0
    move-object v6, v4

    goto/16 :goto_3

    :cond_5
    :try_start_0
    invoke-virtual {p1, v4}, Ljava/lang/Class;->getDeclaredConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;

    move-result-object v3
    :try_end_0
    .catch Ljava/lang/NoSuchMethodException; {:try_start_0 .. :try_end_0} :catch_0

    if-eq p0, v5, :cond_7

    sget-object v6, Ld/o$a;->a:Ld/o$a;

    invoke-virtual {v6, v4, v3}, Ld/o$a;->a(Ljava/lang/Object;Ljava/lang/reflect/AccessibleObject;)Z

    move-result v6

    if-eqz v6, :cond_6

    sget-object v6, Lcom/google/gson/p$a;->d:Lcom/google/gson/p$a;

    if-ne p0, v6, :cond_7

    invoke-virtual {v3}, Ljava/lang/reflect/Constructor;->getModifiers()I

    move-result v6

    invoke-static {v6}, Ljava/lang/reflect/Modifier;->isPublic(I)Z

    move-result v6

    if-eqz v6, :cond_6

    goto :goto_1

    :cond_6
    new-instance v3, Ljava/lang/StringBuilder;

    const-string v6, "Unable to invoke no-args constructor of "

    invoke-direct {v3, v6}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string v6, "; constructor is not accessible and ReflectionAccessFilter does not permit making it accessible. Register an InstanceCreator or a TypeAdapter for this type, change the visibility of the constructor or adjust the access filter."

    invoke-virtual {v3, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    new-instance v6, Ld/f;

    invoke-direct {v6, v3}, Ld/f;-><init>(Ljava/lang/String;)V

    goto :goto_3

    :cond_7
    :goto_1
    if-ne p0, v5, :cond_8

    sget-object v6, Lg/a;->a:Lg/a$a;

    :try_start_1
    invoke-virtual {v3, v1}, Ljava/lang/reflect/AccessibleObject;->setAccessible(Z)V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1

    move-object v6, v4

    goto :goto_2

    :catch_1
    move-exception v6

    new-instance v7, Ljava/lang/StringBuilder;

    const-string v8, "Failed making constructor \'"

    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {v3}, Lg/a;->b(Ljava/lang/reflect/Constructor;)Ljava/lang/String;

    move-result-object v8

    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v8, "\' accessible; either increase its visibility or write a custom InstanceCreator or TypeAdapter for its declaring type: "

    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v6}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v6

    invoke-virtual {v7, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v6

    :goto_2
    if-eqz v6, :cond_8

    new-instance v3, Ld/g;

    invoke-direct {v3, v6}, Ld/g;-><init>(Ljava/lang/String;)V

    move-object v6, v3

    goto :goto_3

    :cond_8
    new-instance v6, Ld/h;

    invoke-direct {v6, v3}, Ld/h;-><init>(Ljava/lang/reflect/Constructor;)V

    :goto_3
    if-eqz v6, :cond_9

    return-object v6

    :cond_9
    const-class v3, Ljava/util/Collection;

    invoke-virtual {v3, p1}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z

    move-result v3

    if-eqz v3, :cond_d

    const-class v0, Ljava/util/SortedSet;

    invoke-virtual {v0, p1}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z

    move-result v0

    if-eqz v0, :cond_a

    new-instance v4, Ld/a;

    invoke-direct {v4, v1}, Ld/a;-><init>(I)V

    goto/16 :goto_4

    :cond_a
    const-class v0, Ljava/util/Set;

    invoke-virtual {v0, p1}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z

    move-result v0

    if-eqz v0, :cond_b

    new-instance v4, Ld/a;

    const/4 v0, 0x2

    invoke-direct {v4, v0}, Ld/a;-><init>(I)V

    goto/16 :goto_4

    :cond_b
    const-class v0, Ljava/util/Queue;

    invoke-virtual {v0, p1}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z

    move-result v0

    if-eqz v0, :cond_c

    new-instance v4, Ld/a;

    const/4 v0, 0x3

    invoke-direct {v4, v0}, Ld/a;-><init>(I)V

    goto/16 :goto_4

    :cond_c
    new-instance v4, Ld/a;

    const/4 v0, 0x4

    invoke-direct {v4, v0}, Ld/a;-><init>(I)V

    goto :goto_4

    :cond_d
    const-class v1, Ljava/util/Map;

    invoke-virtual {v1, p1}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z

    move-result v1

    if-eqz v1, :cond_12

    const-class v1, Ljava/util/concurrent/ConcurrentNavigableMap;

    invoke-virtual {v1, p1}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z

    move-result v1

    if-eqz v1, :cond_e

    new-instance v4, Ld/a;

    const/4 v0, 0x5

    invoke-direct {v4, v0}, Ld/a;-><init>(I)V

    goto :goto_4

    :cond_e
    const-class v1, Ljava/util/concurrent/ConcurrentMap;

    invoke-virtual {v1, p1}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z

    move-result v1

    if-eqz v1, :cond_f

    new-instance v4, Ld/a;

    const/4 v0, 0x6

    invoke-direct {v4, v0}, Ld/a;-><init>(I)V

    goto :goto_4

    :cond_f
    const-class v1, Ljava/util/SortedMap;

    invoke-virtual {v1, p1}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z

    move-result v1

    if-eqz v1, :cond_10

    new-instance v4, Ld/a;

    const/4 v0, 0x7

    invoke-direct {v4, v0}, Ld/a;-><init>(I)V

    goto :goto_4

    :cond_10
    instance-of v1, v2, Ljava/lang/reflect/ParameterizedType;

    if-eqz v1, :cond_11

    check-cast v2, Ljava/lang/reflect/ParameterizedType;

    invoke-interface {v2}, Ljava/lang/reflect/ParameterizedType;->getActualTypeArguments()[Ljava/lang/reflect/Type;

    move-result-object v1

    aget-object v0, v1, v0

    invoke-static {v0}, Ljava/util/Objects;->requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;

    invoke-static {v0}, Ld/b;->a(Ljava/lang/reflect/Type;)Ljava/lang/reflect/Type;

    move-result-object v0

    invoke-static {v0}, Ld/b;->e(Ljava/lang/reflect/Type;)Ljava/lang/Class;

    move-result-object v1

    invoke-virtual {v0}, Ljava/lang/Object;->hashCode()I

    const-class v0, Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/Class;->isAssignableFrom(Ljava/lang/Class;)Z

    move-result v0

    if-nez v0, :cond_11

    new-instance v4, Ld/a;

    const/16 v0, 0x8

    invoke-direct {v4, v0}, Ld/a;-><init>(I)V

    goto :goto_4

    :cond_11
    new-instance v4, Ld/a;

    const/16 v0, 0x9

    invoke-direct {v4, v0}, Ld/a;-><init>(I)V

    :cond_12
    :goto_4
    if-eqz v4, :cond_13

    return-object v4

    :cond_13
    invoke-static {p1}, Ld/d;->a(Ljava/lang/Class;)Ljava/lang/String;

    move-result-object v0

    if-eqz v0, :cond_14

    new-instance p0, Ld/d$c;

    invoke-direct {p0, v0}, Ld/d$c;-><init>(Ljava/lang/String;)V

    return-object p0

    :cond_14
    if-ne p0, v5, :cond_15

    new-instance p0, Ld/c;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Ld/c;->a:Ljava/io/Serializable;

    return-object p0

    :cond_15
    new-instance p0, Ljava/lang/StringBuilder;

    const-string v0, "Unable to create instance of "

    invoke-direct {p0, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string p1, "; ReflectionAccessFilter does not permit using reflection or Unsafe. Register an InstanceCreator or a TypeAdapter for this type or adjust the access filter to allow using reflection."

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    new-instance p1, Ld/d$d;

    invoke-direct {p1, p0}, Ld/d$d;-><init>(Ljava/lang/String;)V

    return-object p1
.end method

.method public final toString()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Ld/d;->a:Ljava/util/Map;

    invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method
