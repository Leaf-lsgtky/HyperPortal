.class public Lcom/miui/contentcatcher/service/StartContentCatcherService;
.super Landroid/app/Service;
.source "SourceFile"


# instance fields
.field public a:Lcom/miui/contentcatcher/ContentCatcherService;


# direct methods
.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Landroid/app/Service;-><init>()V

    return-void
.end method


# virtual methods
.method public final onBind(Landroid/content/Intent;)Landroid/os/IBinder;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/service/StartContentCatcherService;->a:Lcom/miui/contentcatcher/ContentCatcherService;

    return-object p0
.end method

.method public final onCreate()V
    .locals 2

    const-string v0, "StartContentCatcherService"

    :try_start_0
    const-string v1, "StartContentCatcherService is success"

    invoke-static {v0, v1}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v1, Lcom/miui/contentcatcher/ContentCatcherService;

    invoke-direct {v1, p0}, Lcom/miui/contentcatcher/ContentCatcherService;-><init>(Landroid/content/Context;)V

    iput-object v1, p0, Lcom/miui/contentcatcher/service/StartContentCatcherService;->a:Lcom/miui/contentcatcher/ContentCatcherService;

    invoke-super {p0}, Landroid/app/Service;->onCreate()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p0

    invoke-virtual {p0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v1

    invoke-static {v0, v1, p0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    :goto_0
    return-void
.end method
