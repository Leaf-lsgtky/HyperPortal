.class public final Lcom/miui/contentcatcher/Interceptor$a;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Landroid/os/Handler$Callback;


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/miui/contentcatcher/Interceptor;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = null
.end annotation


# instance fields
.field public final synthetic a:Lcom/miui/contentcatcher/Interceptor;


# direct methods
.method public constructor <init>(Lcom/miui/contentcatcher/Interceptor;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/miui/contentcatcher/Interceptor$a;->a:Lcom/miui/contentcatcher/Interceptor;

    return-void
.end method


# virtual methods
.method public final handleMessage(Landroid/os/Message;)Z
    .locals 6

    iget v0, p1, Landroid/os/Message;->what:I

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor$a;->a:Lcom/miui/contentcatcher/Interceptor;

    const/4 v1, 0x1

    const-string v2, "Interceptor_Catcher"

    const-string v3, "MSG_CONTENT_: "

    if-eq v0, v1, :cond_1

    const/4 v4, 0x2

    if-eq v0, v4, :cond_0

    const/4 v4, 0x4

    if-eq v0, v4, :cond_1

    const/16 v3, 0x101

    const/4 v4, 0x0

    const-string v5, "MSG_EVENT_: "

    packed-switch v0, :pswitch_data_0

    const/4 p0, 0x0

    return p0

    :pswitch_0
    :try_start_0
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget p1, p1, Landroid/os/Message;->what:I

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v2, p1}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p1, Lmiui/contentcatcher/sdk/Content;

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;

    invoke-direct {p1, v3, v4, p0, v4}, Lmiui/contentcatcher/sdk/Content;-><init>(ILjava/lang/String;Lmiui/contentcatcher/sdk/Token;Ljava/util/Map;)V

    invoke-static {}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->getInstance()Lmiui/contentcatcher/sdk/ContentCatcherManager;

    move-result-object p0

    invoke-virtual {p0, p1}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->onContentCatched(Lmiui/contentcatcher/sdk/Content;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_2

    :catch_0
    move-exception p0

    goto :goto_0

    :catch_1
    move-exception p0

    goto :goto_1

    :goto_0
    const-string p1, "MSG_EVENT_MOVE_OVER-Error: "

    invoke-static {p1, p0, v2}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_2

    :goto_1
    const-string p1, "MSG_EVENT_MOVE_OVER-Exception: "

    invoke-static {p1, p0, v2}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :goto_2
    return v1

    :pswitch_1
    :try_start_1
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget v3, p1, Landroid/os/Message;->what:I

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v2, v0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    iget-object p1, p1, Landroid/os/Message;->obj:Ljava/lang/Object;

    check-cast p1, Landroid/view/MotionEvent;

    const-string v3, "MotionEvent"

    invoke-virtual {v0, v3, p1}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    new-instance v3, Lmiui/contentcatcher/sdk/Content;

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;

    const/16 v5, 0x20

    invoke-direct {v3, v5, v4, p0, v0}, Lmiui/contentcatcher/sdk/Content;-><init>(ILjava/lang/String;Lmiui/contentcatcher/sdk/Token;Ljava/util/Map;)V

    invoke-static {}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->getInstance()Lmiui/contentcatcher/sdk/ContentCatcherManager;

    move-result-object p0

    invoke-virtual {p0, v3}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->onContentCatched(Lmiui/contentcatcher/sdk/Content;)V

    invoke-virtual {p1}, Landroid/view/MotionEvent;->recycle()V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_3
    .catch Ljava/lang/Error; {:try_start_1 .. :try_end_1} :catch_2

    goto :goto_5

    :catch_2
    move-exception p0

    goto :goto_3

    :catch_3
    move-exception p0

    goto :goto_4

    :goto_3
    const-string p1, "MSG_EVENT_MOTION_EVENT-Error: "

    invoke-static {p1, p0, v2}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_5

    :goto_4
    const-string p1, "MSG_EVENT_MOTION_EVENT-Exception: "

    invoke-static {p1, p0, v2}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :goto_5
    return v1

    :pswitch_2
    :try_start_2
    new-instance p0, Ljava/lang/StringBuilder;

    invoke-direct {p0, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget v0, p1, Landroid/os/Message;->what:I

    invoke-virtual {p0, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v2, p0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p0, Lmiui/contentcatcher/sdk/Content;

    iget-object v0, p1, Landroid/os/Message;->obj:Ljava/lang/Object;

    check-cast v0, Landroid/content/Intent;

    invoke-virtual {p1}, Landroid/os/Message;->getData()Landroid/os/Bundle;

    move-result-object p1

    invoke-direct {p0, v0, p1}, Lmiui/contentcatcher/sdk/Content;-><init>(Landroid/content/Intent;Landroid/os/Bundle;)V

    invoke-static {}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->getInstance()Lmiui/contentcatcher/sdk/ContentCatcherManager;

    move-result-object p1

    invoke-virtual {p1, p0}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->onContentCatched(Lmiui/contentcatcher/sdk/Content;)V
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_5
    .catch Ljava/lang/Error; {:try_start_2 .. :try_end_2} :catch_4

    goto :goto_8

    :catch_4
    move-exception p0

    goto :goto_6

    :catch_5
    move-exception p0

    goto :goto_7

    :goto_6
    const-string p1, "MSG_EVENT_FAV-Error: "

    invoke-static {p1, p0, v2}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_8

    :goto_7
    const-string p1, "MSG_EVENT_FAV-Exception: "

    invoke-static {p1, p0, v2}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :goto_8
    return v1

    :pswitch_3
    :try_start_3
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget p1, p1, Landroid/os/Message;->what:I

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v2, p1}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p1, Lmiui/contentcatcher/sdk/Content;

    iget-object p0, p0, Lcom/miui/contentcatcher/Interceptor;->c:Lmiui/contentcatcher/sdk/Token;

    invoke-direct {p1, v3, v4, p0, v4}, Lmiui/contentcatcher/sdk/Content;-><init>(ILjava/lang/String;Lmiui/contentcatcher/sdk/Token;Ljava/util/Map;)V

    invoke-static {}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->getInstance()Lmiui/contentcatcher/sdk/ContentCatcherManager;

    move-result-object p0

    invoke-virtual {p0, p1}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->onContentCatched(Lmiui/contentcatcher/sdk/Content;)V
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_7
    .catch Ljava/lang/Error; {:try_start_3 .. :try_end_3} :catch_6

    goto :goto_b

    :catch_6
    move-exception p0

    goto :goto_9

    :catch_7
    move-exception p0

    goto :goto_a

    :goto_9
    const-string p1, "MSG_EVENT_BACK-Error: "

    invoke-static {p1, p0, v2}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_b

    :goto_a
    const-string p1, "MSG_EVENT_BACK-Exception: "

    invoke-static {p1, p0, v2}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :goto_b
    return v1

    :cond_0
    :try_start_4
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget p1, p1, Landroid/os/Message;->what:I

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v2, p1}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->m()V
    :try_end_4
    .catch Ljava/lang/Exception; {:try_start_4 .. :try_end_4} :catch_9
    .catch Ljava/lang/Error; {:try_start_4 .. :try_end_4} :catch_8

    goto :goto_e

    :catch_8
    move-exception p0

    goto :goto_c

    :catch_9
    move-exception p0

    goto :goto_d

    :goto_c
    const-string p1, "MSG_CONTENT_LOADED-Error: "

    invoke-static {p1, p0, v2}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_e

    :goto_d
    const-string p1, "MSG_CONTENT_LOADED-Exception: "

    invoke-static {p1, p0, v2}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :goto_e
    return v1

    :cond_1
    :try_start_5
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget p1, p1, Landroid/os/Message;->what:I

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {v2, p1}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p1, p0, Lcom/miui/contentcatcher/Interceptor;->e:Ld/c;

    invoke-virtual {p1}, Ld/c;->d()Z

    move-result p1

    if-eqz p1, :cond_2

    invoke-virtual {p0}, Lcom/miui/contentcatcher/Interceptor;->m()V
    :try_end_5
    .catch Ljava/lang/Exception; {:try_start_5 .. :try_end_5} :catch_b
    .catch Ljava/lang/Error; {:try_start_5 .. :try_end_5} :catch_a

    goto :goto_11

    :catch_a
    move-exception p0

    goto :goto_f

    :catch_b
    move-exception p0

    goto :goto_10

    :goto_f
    const-string p1, "MSG_CONTENT_UPDATED-Error: "

    invoke-static {p1, p0, v2}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_11

    :goto_10
    const-string p1, "MSG_CONTENT_UPDATED-Exception: "

    invoke-static {p1, p0, v2}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_2
    :goto_11
    return v1

    :pswitch_data_0
    .packed-switch 0x65
        :pswitch_3
        :pswitch_2
        :pswitch_1
        :pswitch_0
        :pswitch_3
    .end packed-switch
.end method
