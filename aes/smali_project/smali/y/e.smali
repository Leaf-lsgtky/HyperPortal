.class public final synthetic Ly/e;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/util/function/Function;


# virtual methods
.method public final apply(Ljava/lang/Object;)Ljava/lang/Object;
    .locals 1

    check-cast p1, Ljava/lang/String;

    new-instance p0, Ljava/lang/StringBuilder;

    const-string v0, "onFetchWebViewText map, text="

    invoke-direct {p0, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    const-string v0, "SettingTrigger"

    invoke-static {v0, p0}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    new-instance p0, Lcom/miui/contentcatcher/common/NodeInfo;

    invoke-direct {p0}, Lcom/miui/contentcatcher/common/NodeInfo;-><init>()V

    invoke-virtual {p0, p1}, Lcom/miui/contentcatcher/common/NodeInfo;->setText(Ljava/lang/String;)V

    const-string p1, "webview.text"

    invoke-virtual {p0, p1}, Lcom/miui/contentcatcher/common/NodeInfo;->setClazz(Ljava/lang/String;)V

    const/4 p1, 0x1

    invoke-virtual {p0, p1}, Lcom/miui/contentcatcher/common/NodeInfo;->setType(I)V

    return-object p0
.end method
