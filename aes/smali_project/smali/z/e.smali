.class public final synthetic Lz/e;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Landroid/os/Handler$Callback;


# instance fields
.field public final synthetic a:Lz/j;


# direct methods
.method public synthetic constructor <init>(Lz/j;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lz/e;->a:Lz/j;

    return-void
.end method


# virtual methods
.method public final handleMessage(Landroid/os/Message;)Z
    .locals 11

    iget-object p0, p0, Lz/e;->a:Lz/j;

    const-string v0, "findNodesBySelf exception "

    const-string v1, "serializeUIInfoList size "

    const-string v2, "dumpViews exception "

    iget-boolean v3, p0, Lz/j;->b:Z

    const/4 v4, 0x1

    if-eqz v3, :cond_0

    goto/16 :goto_8

    :cond_0
    iget v3, p1, Landroid/os/Message;->what:I

    sget-object v5, Lz/j$b;->b:Lz/j$b;

    sget-object v6, Lz/j$b;->a:Lz/j$b;

    if-nez v3, :cond_3

    iget-object p1, p1, Landroid/os/Message;->obj:Ljava/lang/Object;

    instance-of v0, p1, Lz/j$c;

    if-eqz v0, :cond_b

    check-cast p1, Lz/j$c;

    sget-object v0, Lz/j;->d:Lz/j$b;

    if-eq v0, v6, :cond_2

    iget-object v0, p0, Lz/j;->c:Ljava/util/ArrayDeque;

    invoke-virtual {v0}, Ljava/util/ArrayDeque;->size()I

    move-result v0

    const/4 v1, 0x5

    if-lt v0, v1, :cond_1

    iget-object v0, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v1, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p1, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v2, "too many request!"

    invoke-static {v2, v0, v1, p1}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    goto/16 :goto_7

    :cond_1
    iget-object v0, p0, Lz/j;->c:Ljava/util/ArrayDeque;

    invoke-virtual {v0, p1}, Ljava/util/ArrayDeque;->addLast(Ljava/lang/Object;)V

    goto/16 :goto_7

    :cond_2
    sput-object v5, Lz/j;->d:Lz/j$b;

    invoke-virtual {p0, p1}, Lz/j;->h(Lz/j$c;)Z

    move-result p1

    if-eqz p1, :cond_b

    sput-object v6, Lz/j;->d:Lz/j$b;

    goto/16 :goto_7

    :cond_3
    if-ne v3, v4, :cond_4

    sput-object v6, Lz/j;->d:Lz/j$b;

    goto/16 :goto_7

    :cond_4
    const/4 v7, 0x2

    if-ne v3, v7, :cond_6

    iget-object v0, p0, Lz/j;->a:Landroid/os/Handler;

    invoke-virtual {v0, v7}, Landroid/os/Handler;->removeMessages(I)V

    iget-object p1, p1, Landroid/os/Message;->obj:Ljava/lang/Object;

    instance-of v0, p1, Lz/j$c;

    if-eqz v0, :cond_5

    check-cast p1, Lz/j$c;

    iget-object v0, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v1, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p1, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v2, "dumpViews failed"

    invoke-static {v2, v0, v1, p1}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    :cond_5
    sput-object v6, Lz/j;->d:Lz/j$b;

    goto/16 :goto_7

    :cond_6
    const/4 v8, 0x3

    const-string v9, "Catcher_AICollectorManager"

    if-ne v3, v8, :cond_9

    const-string v0, "dumpViews serialize UIInfo"

    invoke-static {v9, v0}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v0, p0, Lz/j;->a:Landroid/os/Handler;

    invoke-virtual {v0, v7}, Landroid/os/Handler;->removeMessages(I)V

    iget-object p1, p1, Landroid/os/Message;->obj:Ljava/lang/Object;

    instance-of v0, p1, Lz/j$c;

    if-eqz v0, :cond_8

    check-cast p1, Lz/j$c;

    :try_start_0
    const-string v0, "serializeUIInfoTree"

    invoke-static {v0}, Landroid/os/Trace;->beginSection(Ljava/lang/String;)V

    iget-object v0, p1, Lz/j$c;->j:Ljava/lang/Object;

    check-cast v0, Lz/l;

    iget v1, p1, Lz/j$c;->h:I

    sget-object v3, Lz/d;->a:[Ljava/lang/String;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    const-string v3, ""

    if-nez v0, :cond_7

    goto :goto_0

    :cond_7
    :try_start_1
    new-instance v7, Ljava/io/ByteArrayOutputStream;

    invoke-direct {v7}, Ljava/io/ByteArrayOutputStream;-><init>()V

    invoke-static {}, Landroid/util/Xml;->newSerializer()Lorg/xmlpull/v1/XmlSerializer;

    move-result-object v8

    const-string v9, "http://xmlpull.org/v1/doc/features.html#indent-output"

    const/4 v10, 0x0

    invoke-interface {v8, v9, v10}, Lorg/xmlpull/v1/XmlSerializer;->setFeature(Ljava/lang/String;Z)V

    const-string v9, "UTF-8"

    invoke-interface {v8, v7, v9}, Lorg/xmlpull/v1/XmlSerializer;->setOutput(Ljava/io/OutputStream;Ljava/lang/String;)V

    sget-object v10, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    invoke-interface {v8, v9, v10}, Lorg/xmlpull/v1/XmlSerializer;->startDocument(Ljava/lang/String;Ljava/lang/Boolean;)V

    const-string v9, "hierarchy"

    invoke-interface {v8, v3, v9}, Lorg/xmlpull/v1/XmlSerializer;->startTag(Ljava/lang/String;Ljava/lang/String;)Lorg/xmlpull/v1/XmlSerializer;

    const-string v10, "rotation"

    invoke-static {v1}, Ljava/lang/Integer;->toString(I)Ljava/lang/String;

    move-result-object v1

    invoke-interface {v8, v3, v10, v1}, Lorg/xmlpull/v1/XmlSerializer;->attribute(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lorg/xmlpull/v1/XmlSerializer;

    const-string v1, "root"

    invoke-interface {v8, v3, v1}, Lorg/xmlpull/v1/XmlSerializer;->startTag(Ljava/lang/String;Ljava/lang/String;)Lorg/xmlpull/v1/XmlSerializer;

    invoke-static {v0, v8}, Lz/d;->e(Lz/l;Lorg/xmlpull/v1/XmlSerializer;)V

    invoke-interface {v8, v3, v1}, Lorg/xmlpull/v1/XmlSerializer;->endTag(Ljava/lang/String;Ljava/lang/String;)Lorg/xmlpull/v1/XmlSerializer;

    invoke-interface {v8, v3, v9}, Lorg/xmlpull/v1/XmlSerializer;->endTag(Ljava/lang/String;Ljava/lang/String;)Lorg/xmlpull/v1/XmlSerializer;

    invoke-interface {v8}, Lorg/xmlpull/v1/XmlSerializer;->endDocument()V

    invoke-virtual {v7}, Ljava/io/ByteArrayOutputStream;->toString()Ljava/lang/String;

    move-result-object v3

    :goto_0
    iget-object v0, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v1, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget v7, p1, Lz/j$c;->g:I

    iget-object v8, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v0, v1, v3, v7, v8}, Lz/k;->c(Ljava/lang/String;Landroid/os/Bundle;Ljava/lang/String;ILmiui/security/IUIAgentCallback;)V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_0
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    :goto_1
    invoke-static {}, Landroid/os/Trace;->endSection()V

    goto :goto_3

    :catchall_0
    move-exception p0

    goto :goto_2

    :catch_0
    move-exception v0

    :try_start_2
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    iget-object v1, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v2, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p1, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v0, v1, v2, p1}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    goto :goto_1

    :goto_2
    invoke-static {}, Landroid/os/Trace;->endSection()V

    throw p0

    :cond_8
    :goto_3
    sput-object v6, Lz/j;->d:Lz/j$b;

    goto :goto_7

    :cond_9
    const/4 v2, 0x4

    if-ne v3, v2, :cond_b

    const-string v2, "findNodesBySelf serialize UIInfo"

    invoke-static {v9, v2}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v2, p0, Lz/j;->a:Landroid/os/Handler;

    invoke-virtual {v2, v7}, Landroid/os/Handler;->removeMessages(I)V

    iget-object p1, p1, Landroid/os/Message;->obj:Ljava/lang/Object;

    instance-of v2, p1, Lz/j$c;

    if-eqz v2, :cond_a

    check-cast p1, Lz/j$c;

    :try_start_3
    const-string v2, "serializeUIInfoList"

    invoke-static {v2}, Landroid/os/Trace;->beginSection(Ljava/lang/String;)V

    iget-object v2, p1, Lz/j$c;->j:Ljava/lang/Object;

    check-cast v2, Ljava/util/ArrayList;

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I

    move-result v1

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v9, v1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget v1, p1, Lz/j$c;->h:I

    new-instance v3, Lz/c;

    invoke-direct {v3, v2}, Lz/c;-><init>(Ljava/util/ArrayList;)V

    invoke-static {v3, v1}, Lz/d;->f(Lz/d$a;I)Ljava/lang/String;

    move-result-object v1

    iget-object v2, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v3, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget v7, p1, Lz/j$c;->g:I

    iget-object v8, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v2, v3, v1, v7, v8}, Lz/k;->c(Ljava/lang/String;Landroid/os/Bundle;Ljava/lang/String;ILmiui/security/IUIAgentCallback;)V
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_1
    .catchall {:try_start_3 .. :try_end_3} :catchall_1

    :goto_4
    invoke-static {}, Landroid/os/Trace;->endSection()V

    goto :goto_6

    :catchall_1
    move-exception p0

    goto :goto_5

    :catch_1
    move-exception v1

    :try_start_4
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    iget-object v1, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v2, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p1, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v0, v1, v2, p1}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_1

    goto :goto_4

    :goto_5
    invoke-static {}, Landroid/os/Trace;->endSection()V

    throw p0

    :cond_a
    :goto_6
    sput-object v6, Lz/j;->d:Lz/j$b;

    :cond_b
    :goto_7
    sget-object p1, Lz/j;->d:Lz/j$b;

    if-ne p1, v6, :cond_d

    iget-object p1, p0, Lz/j;->c:Ljava/util/ArrayDeque;

    invoke-virtual {p1}, Ljava/util/ArrayDeque;->isEmpty()Z

    move-result p1

    if-nez p1, :cond_d

    iget-object p1, p0, Lz/j;->c:Ljava/util/ArrayDeque;

    invoke-virtual {p1}, Ljava/util/ArrayDeque;->pollFirst()Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Lz/j$c;

    if-eqz p1, :cond_c

    iget-object v0, p1, Lz/j$c;->c:Ljava/lang/ref/WeakReference;

    invoke-virtual {v0}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v0

    if-nez v0, :cond_c

    iget-object v0, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v1, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p1, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v2, "activity destroyed"

    invoke-static {v2, v0, v1, p1}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    goto :goto_7

    :cond_c
    sput-object v5, Lz/j;->d:Lz/j$b;

    invoke-virtual {p0, p1}, Lz/j;->h(Lz/j$c;)Z

    move-result p1

    if-eqz p1, :cond_b

    sput-object v6, Lz/j;->d:Lz/j$b;

    goto :goto_7

    :cond_d
    :goto_8
    return v4
.end method
