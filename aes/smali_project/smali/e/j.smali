.class public final Le/j;
.super Le/k$b;
.source "SourceFile"


# instance fields
.field public final synthetic f:Z

.field public final synthetic g:Ljava/lang/reflect/Method;

.field public final synthetic h:Z

.field public final synthetic i:Lcom/google/gson/s;

.field public final synthetic j:Lcom/google/gson/d;

.field public final synthetic k:Li/a;

.field public final synthetic l:Z

.field public final synthetic m:Z


# direct methods
.method public constructor <init>(Ljava/lang/String;Ljava/lang/reflect/Field;ZZZLjava/lang/reflect/Method;ZLcom/google/gson/s;Lcom/google/gson/d;Li/a;ZZ)V
    .locals 0

    iput-boolean p5, p0, Le/j;->f:Z

    iput-object p6, p0, Le/j;->g:Ljava/lang/reflect/Method;

    iput-boolean p7, p0, Le/j;->h:Z

    iput-object p8, p0, Le/j;->i:Lcom/google/gson/s;

    iput-object p9, p0, Le/j;->j:Lcom/google/gson/d;

    iput-object p10, p0, Le/j;->k:Li/a;

    iput-boolean p11, p0, Le/j;->l:Z

    iput-boolean p12, p0, Le/j;->m:Z

    invoke-direct {p0, p1, p2, p3, p4}, Le/k$b;-><init>(Ljava/lang/String;Ljava/lang/reflect/Field;ZZ)V

    return-void
.end method


# virtual methods
.method public final a(Lj/a;I[Ljava/lang/Object;)V
    .locals 2
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;,
            Lcom/google/gson/l;
        }
    .end annotation

    iget-object v0, p0, Le/j;->i:Lcom/google/gson/s;

    invoke-virtual {v0, p1}, Lcom/google/gson/s;->a(Lj/a;)Ljava/lang/Object;

    move-result-object v0

    if-nez v0, :cond_1

    iget-boolean v1, p0, Le/j;->l:Z

    if-nez v1, :cond_0

    goto :goto_0

    :cond_0
    new-instance p2, Lcom/google/gson/l;

    new-instance p3, Ljava/lang/StringBuilder;

    const-string v0, "null is not allowed as value for record component \'"

    invoke-direct {p3, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object p0, p0, Le/k$b;->c:Ljava/lang/String;

    invoke-virtual {p3, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p0, "\' of primitive type; at path "

    invoke-virtual {p3, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Lj/a;->h()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {p3, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-direct {p2, p0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw p2

    :cond_1
    :goto_0
    aput-object v0, p3, p2

    return-void
.end method

.method public final b(Lj/a;Ljava/lang/Object;)V
    .locals 2
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;,
            Ljava/lang/IllegalAccessException;
        }
    .end annotation

    iget-object v0, p0, Le/j;->i:Lcom/google/gson/s;

    invoke-virtual {v0, p1}, Lcom/google/gson/s;->a(Lj/a;)Ljava/lang/Object;

    move-result-object p1

    if-nez p1, :cond_0

    iget-boolean v0, p0, Le/j;->l:Z

    if-nez v0, :cond_2

    :cond_0
    iget-boolean v0, p0, Le/j;->f:Z

    iget-object v1, p0, Le/k$b;->b:Ljava/lang/reflect/Field;

    if-eqz v0, :cond_1

    invoke-static {p2, v1}, Le/k;->b(Ljava/lang/Object;Ljava/lang/reflect/AccessibleObject;)V

    goto :goto_0

    :cond_1
    iget-boolean p0, p0, Le/j;->m:Z

    if-nez p0, :cond_3

    :goto_0
    invoke-virtual {v1, p2, p1}, Ljava/lang/reflect/Field;->set(Ljava/lang/Object;Ljava/lang/Object;)V

    :cond_2
    return-void

    :cond_3
    const/4 p0, 0x0

    invoke-static {v1, p0}, Lg/a;->d(Ljava/lang/reflect/AccessibleObject;Z)Ljava/lang/String;

    move-result-object p0

    new-instance p1, Lcom/google/gson/i;

    new-instance p2, Ljava/lang/StringBuilder;

    const-string v0, "Cannot set value of \'static final\' "

    invoke-direct {p2, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p2, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-direct {p1, p0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;)V

    throw p1
.end method

.method public final c(Lj/c;Ljava/lang/Object;)V
    .locals 3
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;,
            Ljava/lang/IllegalAccessException;
        }
    .end annotation

    iget-boolean v0, p0, Le/k$b;->d:Z

    if-nez v0, :cond_0

    return-void

    :cond_0
    iget-object v0, p0, Le/k$b;->b:Ljava/lang/reflect/Field;

    iget-boolean v1, p0, Le/j;->f:Z

    iget-object v2, p0, Le/j;->g:Ljava/lang/reflect/Method;

    if-eqz v1, :cond_2

    if-nez v2, :cond_1

    invoke-static {p2, v0}, Le/k;->b(Ljava/lang/Object;Ljava/lang/reflect/AccessibleObject;)V

    goto :goto_0

    :cond_1
    invoke-static {p2, v2}, Le/k;->b(Ljava/lang/Object;Ljava/lang/reflect/AccessibleObject;)V

    :cond_2
    :goto_0
    if-eqz v2, :cond_3

    const/4 v0, 0x0

    :try_start_0
    invoke-virtual {v2, p2, v0}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0
    :try_end_0
    .catch Ljava/lang/reflect/InvocationTargetException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_1

    :catch_0
    move-exception p0

    const/4 p1, 0x0

    invoke-static {v2, p1}, Lg/a;->d(Ljava/lang/reflect/AccessibleObject;Z)Ljava/lang/String;

    move-result-object p1

    new-instance p2, Lcom/google/gson/i;

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "Accessor "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p1, " threw exception"

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {p0}, Ljava/lang/reflect/InvocationTargetException;->getCause()Ljava/lang/Throwable;

    move-result-object p0

    invoke-direct {p2, p1, p0}, Ljava/lang/RuntimeException;-><init>(Ljava/lang/String;Ljava/lang/Throwable;)V

    throw p2

    :cond_3
    invoke-virtual {v0, p2}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    :goto_1
    if-ne v0, p2, :cond_4

    return-void

    :cond_4
    iget-object p2, p0, Le/k$b;->a:Ljava/lang/String;

    invoke-virtual {p1, p2}, Lj/c;->g(Ljava/lang/String;)V

    iget-boolean p2, p0, Le/j;->h:Z

    iget-object v1, p0, Le/j;->i:Lcom/google/gson/s;

    if-eqz p2, :cond_5

    goto :goto_2

    :cond_5
    new-instance p2, Le/n;

    iget-object v2, p0, Le/j;->k:Li/a;

    iget-object p0, p0, Le/j;->j:Lcom/google/gson/d;

    iget-object v2, v2, Li/a;->b:Ljava/lang/reflect/Type;

    invoke-direct {p2, p0, v1, v2}, Le/n;-><init>(Lcom/google/gson/d;Lcom/google/gson/s;Ljava/lang/reflect/Type;)V

    move-object v1, p2

    :goto_2
    invoke-virtual {v1, p1, v0}, Lcom/google/gson/s;->b(Lj/c;Ljava/lang/Object;)V

    return-void
.end method
