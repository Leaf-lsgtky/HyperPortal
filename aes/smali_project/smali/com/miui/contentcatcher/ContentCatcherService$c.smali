.class public final Lcom/miui/contentcatcher/ContentCatcherService$c;
.super Landroid/os/Handler;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/miui/contentcatcher/ContentCatcherService;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = "c"
.end annotation


# instance fields
.field public final synthetic a:Lcom/miui/contentcatcher/ContentCatcherService;


# direct methods
.method public constructor <init>(Lcom/miui/contentcatcher/ContentCatcherService;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/ContentCatcherService$c;->a:Lcom/miui/contentcatcher/ContentCatcherService;

    invoke-direct {p0}, Landroid/os/Handler;-><init>()V

    return-void
.end method


# virtual methods
.method public final handleMessage(Landroid/os/Message;)V
    .locals 2

    iget v0, p1, Landroid/os/Message;->what:I

    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService$c;->a:Lcom/miui/contentcatcher/ContentCatcherService;

    const-string v1, "ContentCatcherService"

    packed-switch v0, :pswitch_data_0

    goto/16 :goto_6

    :pswitch_0
    :try_start_0
    iget-object p1, p1, Landroid/os/Message;->obj:Ljava/lang/Object;

    if-eqz p1, :cond_0

    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->a:Lw/a;

    if-eqz p0, :cond_0

    check-cast p1, Ljava/lang/String;

    invoke-virtual {p0, p1}, Lw/a;->f(Ljava/lang/String;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Error; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_6

    :catch_0
    move-exception p0

    goto :goto_0

    :catch_1
    move-exception p0

    goto :goto_1

    :goto_0
    const-string p1, "SEND_PENDING_RESULT-Error: "

    invoke-static {p1, p0, v1}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_6

    :goto_1
    const-string p1, "SEND_PENDING_RESULT-Exception: "

    invoke-static {p1, p0, v1}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    goto :goto_6

    :pswitch_1
    :try_start_1
    iget-object p1, p1, Landroid/os/Message;->obj:Ljava/lang/Object;

    if-eqz p1, :cond_0

    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->a:Lw/a;

    if-eqz p0, :cond_0

    check-cast p1, Ljava/lang/String;

    iget-object p0, p0, Lw/a;->c:Ljava/util/HashMap;

    invoke-virtual {p0, p1}, Ljava/util/HashMap;->remove(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Lmiui/contentcatcher/sdk/ClientCatcherResult;
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_3
    .catch Ljava/lang/Error; {:try_start_1 .. :try_end_1} :catch_2

    goto :goto_6

    :catch_2
    move-exception p0

    goto :goto_2

    :catch_3
    move-exception p0

    goto :goto_3

    :goto_2
    const-string p1, "REMOVE_PENDING_RESULT-Error: "

    invoke-static {p1, p0, v1}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_6

    :goto_3
    const-string p1, "REMOVE_PENDING_RESULT-Exception: "

    invoke-static {p1, p0, v1}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    goto :goto_6

    :pswitch_2
    :try_start_2
    iget-object p1, p1, Landroid/os/Message;->obj:Ljava/lang/Object;

    if-eqz p1, :cond_0

    iget-object p0, p0, Lcom/miui/contentcatcher/ContentCatcherService;->a:Lw/a;

    if-eqz p0, :cond_0

    check-cast p1, Lmiui/contentcatcher/sdk/Content;

    invoke-virtual {p0, p1}, Lw/a;->c(Lmiui/contentcatcher/sdk/Content;)V
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_5
    .catch Ljava/lang/Error; {:try_start_2 .. :try_end_2} :catch_4

    goto :goto_6

    :catch_4
    move-exception p0

    goto :goto_4

    :catch_5
    move-exception p0

    goto :goto_5

    :goto_4
    const-string p1, "CONTENT_CATCHED-Error: "

    invoke-static {p1, p0, v1}, Lk/a;->a(Ljava/lang/String;Ljava/lang/Error;Ljava/lang/String;)V

    goto :goto_6

    :goto_5
    const-string p1, "CONTENT_CATCHED-Exception: "

    invoke-static {p1, p0, v1}, Lk/a;->b(Ljava/lang/String;Ljava/lang/Exception;Ljava/lang/String;)V

    :cond_0
    :goto_6
    return-void

    :pswitch_data_0
    .packed-switch 0x64
        :pswitch_2
        :pswitch_1
        :pswitch_0
    .end packed-switch
.end method
