.class public final Lcom/miui/contentcatcher/ContentCatcherService$a;
.super Landroid/content/IOnPrimaryClipChangedListener$Stub;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/miui/contentcatcher/ContentCatcherService;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = null
.end annotation


# virtual methods
.method public final dispatchPrimaryClipChanged()V
    .locals 3

    new-instance p0, Ljava/util/HashMap;

    invoke-direct {p0}, Ljava/util/HashMap;-><init>()V

    const-string v0, "isClipboard"

    const-string v1, "clipboard"

    invoke-virtual {p0, v0, v1}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    invoke-virtual {v0, p0}, Ljava/util/HashMap;->putAll(Ljava/util/Map;)V

    new-instance p0, Lmiui/contentcatcher/sdk/Content;

    const/16 v1, 0x160

    const/4 v2, 0x0

    invoke-direct {p0, v1, v2, v2, v0}, Lmiui/contentcatcher/sdk/Content;-><init>(ILjava/lang/String;Lmiui/contentcatcher/sdk/Token;Ljava/util/Map;)V

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "dispatchContent, content: "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "CatcherHolder"

    invoke-static {v1, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->getInstance()Lmiui/contentcatcher/sdk/ContentCatcherManager;

    move-result-object v0

    invoke-virtual {v0, p0}, Lmiui/contentcatcher/sdk/ContentCatcherManager;->onContentCatched(Lmiui/contentcatcher/sdk/Content;)V

    return-void
.end method
