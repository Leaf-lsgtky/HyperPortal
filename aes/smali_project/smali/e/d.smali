.class public final Le/d;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Lcom/google/gson/t;


# instance fields
.field public final synthetic a:I

.field public final b:Ljava/lang/Object;


# direct methods
.method public synthetic constructor <init>(Ljava/lang/Object;I)V
    .locals 0

    iput p2, p0, Le/d;->a:I

    iput-object p1, p0, Le/d;->b:Ljava/lang/Object;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static b(Ld/d;Lcom/google/gson/d;Li/a;Lc/a;)Lcom/google/gson/s;
    .locals 6

    invoke-interface {p3}, Lc/a;->value()Ljava/lang/Class;

    move-result-object v0

    new-instance v1, Li/a;

    invoke-direct {v1, v0}, Li/a;-><init>(Ljava/lang/reflect/Type;)V

    invoke-virtual {p0, v1}, Ld/d;->b(Li/a;)Ld/n;

    move-result-object p0

    invoke-interface {p0}, Ld/n;->a()Ljava/lang/Object;

    move-result-object p0

    invoke-interface {p3}, Lc/a;->nullSafe()Z

    move-result v5

    instance-of p3, p0, Lcom/google/gson/s;

    if-eqz p3, :cond_0

    check-cast p0, Lcom/google/gson/s;

    goto :goto_3

    :cond_0
    instance-of p3, p0, Lcom/google/gson/t;

    if-eqz p3, :cond_1

    check-cast p0, Lcom/google/gson/t;

    invoke-interface {p0, p1, p2}, Lcom/google/gson/t;->a(Lcom/google/gson/d;Li/a;)Lcom/google/gson/s;

    move-result-object p0

    goto :goto_3

    :cond_1
    instance-of p3, p0, Lcom/google/gson/n;

    if-nez p3, :cond_3

    instance-of v0, p0, Lcom/google/gson/g;

    if-eqz v0, :cond_2

    goto :goto_0

    :cond_2
    new-instance p1, Ljava/lang/IllegalArgumentException;

    new-instance p3, Ljava/lang/StringBuilder;

    const-string v0, "Invalid attempt to bind an instance of "

    invoke-direct {p3, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {p3, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p0, " as a @JsonAdapter for "

    invoke-virtual {p3, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object p0, p2, Li/a;->b:Ljava/lang/reflect/Type;

    invoke-static {p0}, Ld/b;->g(Ljava/lang/reflect/Type;)Ljava/lang/String;

    move-result-object p0

    invoke-virtual {p3, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p0, ". @JsonAdapter value must be a TypeAdapter, TypeAdapterFactory, JsonSerializer or JsonDeserializer."

    invoke-virtual {p3, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-direct {p1, p0}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    throw p1

    :cond_3
    :goto_0
    const/4 v0, 0x0

    if-eqz p3, :cond_4

    move-object p3, p0

    check-cast p3, Lcom/google/gson/n;

    move-object v1, p3

    goto :goto_1

    :cond_4
    move-object v1, v0

    :goto_1
    instance-of p3, p0, Lcom/google/gson/g;

    if-eqz p3, :cond_5

    check-cast p0, Lcom/google/gson/g;

    move-object v2, p0

    goto :goto_2

    :cond_5
    move-object v2, v0

    :goto_2
    new-instance p0, Le/m;

    move-object v0, p0

    move-object v3, p1

    move-object v4, p2

    invoke-direct/range {v0 .. v5}, Le/m;-><init>(Lcom/google/gson/n;Lcom/google/gson/g;Lcom/google/gson/d;Li/a;Z)V

    const/4 v5, 0x0

    :goto_3
    if-eqz p0, :cond_6

    if-eqz v5, :cond_6

    new-instance p1, Lcom/google/gson/c;

    const/4 p2, 0x2

    invoke-direct {p1, p0, p2}, Lcom/google/gson/c;-><init>(Lcom/google/gson/s;I)V

    move-object p0, p1

    :cond_6
    return-object p0
.end method


# virtual methods
.method public final a(Lcom/google/gson/d;Li/a;)Lcom/google/gson/s;
    .locals 2

    iget v0, p0, Le/d;->a:I

    packed-switch v0, :pswitch_data_0

    iget-object p1, p2, Li/a;->a:Ljava/lang/Class;

    const-class p2, Ljava/util/Calendar;

    if-eq p1, p2, :cond_1

    const-class p2, Ljava/util/GregorianCalendar;

    if-ne p1, p2, :cond_0

    goto :goto_0

    :cond_0
    const/4 p0, 0x0

    goto :goto_1

    :cond_1
    :goto_0
    iget-object p0, p0, Le/d;->b:Ljava/lang/Object;

    check-cast p0, Le/o$r;

    :goto_1
    return-object p0

    :pswitch_0
    iget-object p1, p2, Li/a;->a:Ljava/lang/Class;

    const-class p2, Ljava/lang/Number;

    if-ne p1, p2, :cond_2

    iget-object p0, p0, Le/d;->b:Ljava/lang/Object;

    check-cast p0, Le/g;

    goto :goto_2

    :cond_2
    const/4 p0, 0x0

    :goto_2
    return-object p0

    :pswitch_1
    iget-object v0, p2, Li/a;->a:Ljava/lang/Class;

    const-class v1, Lc/a;

    invoke-virtual {v0, v1}, Ljava/lang/Class;->getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;

    move-result-object v0

    check-cast v0, Lc/a;

    if-nez v0, :cond_3

    const/4 p0, 0x0

    goto :goto_3

    :cond_3
    iget-object p0, p0, Le/d;->b:Ljava/lang/Object;

    check-cast p0, Ld/d;

    invoke-static {p0, p1, p2, v0}, Le/d;->b(Ld/d;Lcom/google/gson/d;Li/a;Lc/a;)Lcom/google/gson/s;

    move-result-object p0

    :goto_3
    return-object p0

    nop

    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_1
        :pswitch_0
    .end packed-switch
.end method

.method public toString()Ljava/lang/String;
    .locals 2

    iget v0, p0, Le/d;->a:I

    packed-switch v0, :pswitch_data_0

    invoke-super {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0

    :pswitch_0
    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "Factory[type="

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-class v1, Ljava/util/Calendar;

    invoke-virtual {v1}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "+"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-class v1, Ljava/util/GregorianCalendar;

    invoke-virtual {v1}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, ",adapter="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object p0, p0, Le/d;->b:Ljava/lang/Object;

    check-cast p0, Le/o$r;

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const-string p0, "]"

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0

    :pswitch_data_0
    .packed-switch 0x2
        :pswitch_0
    .end packed-switch
.end method
