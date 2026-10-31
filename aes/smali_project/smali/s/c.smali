.class public final Ls/c;
.super Lmiui/contentcatcher/sdk/WebViewDetector;
.source "SourceFile"


# static fields
.field public static volatile a:Ls/c;


# direct methods
.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Lmiui/contentcatcher/sdk/WebViewDetector;-><init>()V

    return-void
.end method


# virtual methods
.method public onCatch(Ljava/lang/String;)V
    .locals 2
    .annotation runtime Landroid/webkit/JavascriptInterface;
    .end annotation

    .annotation runtime Lcom/uc/webview/export/JavascriptInterface;
    .end annotation

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "SupportWebViewDetector-onCatch: "

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    const-string v1, "SupportWebViewDetector"

    invoke-static {v1, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    invoke-super {p0, p1}, Lmiui/contentcatcher/sdk/WebViewDetector;->onCatch(Ljava/lang/String;)V

    return-void
.end method
