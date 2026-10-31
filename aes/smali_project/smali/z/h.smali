.class public final synthetic Lz/h;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Lcom/xiaomi/aicollector/AICollectorRequest$AICollectorRequestListener;


# instance fields
.field public final synthetic a:Lz/j;

.field public final synthetic b:Lz/j$c;


# direct methods
.method public synthetic constructor <init>(Lz/j;Lz/j$c;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lz/h;->a:Lz/j;

    iput-object p2, p0, Lz/h;->b:Lz/j$c;

    return-void
.end method


# virtual methods
.method public final onRequestFinished(Lcom/xiaomi/aicollector/AICollectorRequest$AICollectorResult;)V
    .locals 6

    iget-object v0, p0, Lz/h;->a:Lz/j;

    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    const-string v1, "aiCollectorResult empty"

    const-string v2, "receive result status: "

    const-string v3, "Catcher_AICollectorManager"

    iget-object p0, p0, Lz/h;->b:Lz/j$c;

    if-eqz p1, :cond_0

    :try_start_0
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p1}, Lcom/xiaomi/aicollector/AICollectorRequest$AICollectorResult;->getStatus()I

    move-result v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v2, ", version: "

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Lcom/xiaomi/aicollector/AICollectorRequest$AICollectorResult;->getVersion()I

    move-result v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v3, v1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    const-string v1, "print result"

    invoke-static {v3, v1}, Lp/b;->j(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v1, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v2, p0, Lz/j$c;->d:Landroid/os/Bundle;

    invoke-virtual {p1}, Lcom/xiaomi/aicollector/AICollectorRequest$AICollectorResult;->getXml()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {p1}, Lcom/xiaomi/aicollector/AICollectorRequest$AICollectorResult;->getVersion()I

    move-result p1

    iget-object v5, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v1, v2, v4, p1, v5}, Lz/k;->c(Ljava/lang/String;Landroid/os/Bundle;Ljava/lang/String;ILmiui/security/IUIAgentCallback;)V

    const-string p1, "callback onResult"

    invoke-static {v3, p1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_1

    :catchall_0
    move-exception p1

    goto :goto_0

    :cond_0
    invoke-static {v1}, Lp/b;->k(Ljava/lang/String;)V

    iget-object p1, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v2, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object v3, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v1, p1, v2, v3}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    goto :goto_1

    :goto_0
    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "startUIAgent onResult error: "

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p1}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-static {p1}, Lp/b;->k(Ljava/lang/String;)V

    iget-object p1, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v1, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p0, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v2, "startUIAgent onResult error"

    invoke-static {v2, p1, v1, p0}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    :goto_1
    iget-object p0, v0, Lz/j;->a:Landroid/os/Handler;

    const/4 p1, 0x1

    invoke-virtual {p0, p1}, Landroid/os/Handler;->obtainMessage(I)Landroid/os/Message;

    move-result-object p0

    invoke-virtual {p0}, Landroid/os/Message;->sendToTarget()V

    return-void
.end method
