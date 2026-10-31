.class public final Ld/a;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ld/n;


# instance fields
.field public final synthetic a:I


# direct methods
.method public synthetic constructor <init>(I)V
    .locals 0

    iput p1, p0, Ld/a;->a:I

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static b(Z)V
    .locals 0

    if-eqz p0, :cond_0

    return-void

    :cond_0
    new-instance p0, Ljava/lang/IllegalArgumentException;

    invoke-direct {p0}, Ljava/lang/IllegalArgumentException;-><init>()V

    throw p0
.end method

.method public static c(Landroid/view/View;)Ls/h;
    .locals 3

    const/4 v0, 0x0

    if-nez p0, :cond_0

    return-object v0

    :cond_0
    instance-of v1, p0, Landroid/webkit/WebView;

    const-string v2, "WrapperFactory"

    if-eqz v1, :cond_1

    const-string v0, "NativeWebViewWrapper"

    invoke-static {v2, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v1, Ls/b;

    invoke-direct {v1, p0}, Ls/h;-><init>(Landroid/view/View;)V

    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/Class;->getCanonicalName()Ljava/lang/String;

    move-result-object p0

    invoke-static {v0, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    return-object v1

    :cond_1
    const-string v1, "com.miui.webkit.WebView"

    invoke-static {p0, v1}, Lmiui/contentcatcher/sdk/utils/WebViewUtils;->checkViewMatchedByClassName(Landroid/view/View;Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_2

    const-string v0, "MiuiWebViewWrapper"

    invoke-static {v2, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v0, Ls/a;

    invoke-direct {v0, p0}, Ls/a;-><init>(Landroid/view/View;)V

    return-object v0

    :cond_2
    const-string v1, "com.tencent.smtt.webkit.WebView"

    invoke-static {p0, v1}, Lmiui/contentcatcher/sdk/utils/WebViewUtils;->checkViewMatchedByClassName(Landroid/view/View;Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_3

    const-string v0, "WeixinWebViewWrapper"

    invoke-static {v2, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v0, Ls/d;

    invoke-direct {v0, p0}, Ls/d;-><init>(Landroid/view/View;)V

    return-object v0

    :cond_3
    const-string v1, "com.tencent.mm.ui.widget.MMWebView"

    invoke-static {p0, v1}, Lmiui/contentcatcher/sdk/utils/WebViewUtils;->checkViewMatchedByClassName(Landroid/view/View;Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_4

    const-string v0, "WeixinWebViewWrapper2"

    invoke-static {v2, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v0, Ls/d;

    invoke-direct {v0, p0}, Ls/d;-><init>(Landroid/view/View;)V

    return-object v0

    :cond_4
    const-string v1, "com.uc.webview.export.WebView"

    invoke-static {p0, v1}, Lmiui/contentcatcher/sdk/utils/WebViewUtils;->checkViewMatchedByClassName(Landroid/view/View;Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_5

    const-string v0, "UCWebViewWrapper"

    invoke-static {v2, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v0, Ls/d;

    invoke-direct {v0, p0}, Ls/d;-><init>(Landroid/view/View;)V

    return-object v0

    :cond_5
    const-string v1, "sogou.webkit.WebView"

    invoke-static {p0, v1}, Lmiui/contentcatcher/sdk/utils/WebViewUtils;->checkViewMatchedByClassName(Landroid/view/View;Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_6

    const-string v0, "SogouWebViewWrapper"

    invoke-static {v2, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v0, Ls/a;

    invoke-direct {v0, p0}, Ls/a;-><init>(Landroid/view/View;)V

    return-object v0

    :cond_6
    const-string v1, "com.baidu.webkit.sdk.WebView"

    invoke-static {p0, v1}, Lmiui/contentcatcher/sdk/utils/WebViewUtils;->checkViewMatchedByClassName(Landroid/view/View;Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_7

    const-string v0, "BaiduWebViewWrapper"

    invoke-static {v2, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v0, Ls/a;

    invoke-direct {v0, p0}, Ls/a;-><init>(Landroid/view/View;)V

    return-object v0

    :cond_7
    const-string v1, "com.tencent.mtt.base.webview.QBWebView"

    invoke-static {p0, v1}, Lmiui/contentcatcher/sdk/utils/WebViewUtils;->checkViewMatchedByClassName(Landroid/view/View;Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_8

    const-string v0, "QQWebViewWrapper"

    invoke-static {v2, v0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    new-instance v0, Ls/d;

    invoke-direct {v0, p0}, Ls/d;-><init>(Landroid/view/View;)V

    return-object v0

    :cond_8
    const-string p0, "OTHER WEBVIEW!"

    invoke-static {v2, p0}, Lp/b;->b(Ljava/lang/String;Ljava/lang/String;)V

    return-object v0
.end method

.method public static d(Ljava/lang/Class;)Ljava/lang/Class;
    .locals 1

    sget-object v0, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    if-ne p0, v0, :cond_0

    const-class p0, Ljava/lang/Integer;

    return-object p0

    :cond_0
    sget-object v0, Ljava/lang/Float;->TYPE:Ljava/lang/Class;

    if-ne p0, v0, :cond_1

    const-class p0, Ljava/lang/Float;

    return-object p0

    :cond_1
    sget-object v0, Ljava/lang/Byte;->TYPE:Ljava/lang/Class;

    if-ne p0, v0, :cond_2

    const-class p0, Ljava/lang/Byte;

    return-object p0

    :cond_2
    sget-object v0, Ljava/lang/Double;->TYPE:Ljava/lang/Class;

    if-ne p0, v0, :cond_3

    const-class p0, Ljava/lang/Double;

    return-object p0

    :cond_3
    sget-object v0, Ljava/lang/Long;->TYPE:Ljava/lang/Class;

    if-ne p0, v0, :cond_4

    const-class p0, Ljava/lang/Long;

    return-object p0

    :cond_4
    sget-object v0, Ljava/lang/Character;->TYPE:Ljava/lang/Class;

    if-ne p0, v0, :cond_5

    const-class p0, Ljava/lang/Character;

    return-object p0

    :cond_5
    sget-object v0, Ljava/lang/Boolean;->TYPE:Ljava/lang/Class;

    if-ne p0, v0, :cond_6

    const-class p0, Ljava/lang/Boolean;

    return-object p0

    :cond_6
    sget-object v0, Ljava/lang/Short;->TYPE:Ljava/lang/Class;

    if-ne p0, v0, :cond_7

    const-class p0, Ljava/lang/Short;

    return-object p0

    :cond_7
    sget-object v0, Ljava/lang/Void;->TYPE:Ljava/lang/Class;

    if-ne p0, v0, :cond_8

    const-class p0, Ljava/lang/Void;

    :cond_8
    return-object p0
.end method


# virtual methods
.method public a()Ljava/lang/Object;
    .locals 0

    iget p0, p0, Ld/a;->a:I

    packed-switch p0, :pswitch_data_0

    new-instance p0, Ld/m;

    invoke-direct {p0}, Ld/m;-><init>()V

    return-object p0

    :pswitch_0
    new-instance p0, Ljava/util/LinkedHashMap;

    invoke-direct {p0}, Ljava/util/LinkedHashMap;-><init>()V

    return-object p0

    :pswitch_1
    new-instance p0, Ljava/util/TreeMap;

    invoke-direct {p0}, Ljava/util/TreeMap;-><init>()V

    return-object p0

    :pswitch_2
    new-instance p0, Ljava/util/concurrent/ConcurrentHashMap;

    invoke-direct {p0}, Ljava/util/concurrent/ConcurrentHashMap;-><init>()V

    return-object p0

    :pswitch_3
    new-instance p0, Ljava/util/concurrent/ConcurrentSkipListMap;

    invoke-direct {p0}, Ljava/util/concurrent/ConcurrentSkipListMap;-><init>()V

    return-object p0

    :pswitch_4
    new-instance p0, Ljava/util/ArrayList;

    invoke-direct {p0}, Ljava/util/ArrayList;-><init>()V

    return-object p0

    :pswitch_5
    new-instance p0, Ljava/util/ArrayDeque;

    invoke-direct {p0}, Ljava/util/ArrayDeque;-><init>()V

    return-object p0

    :pswitch_6
    new-instance p0, Ljava/util/LinkedHashSet;

    invoke-direct {p0}, Ljava/util/LinkedHashSet;-><init>()V

    return-object p0

    :pswitch_7
    new-instance p0, Ljava/util/TreeSet;

    invoke-direct {p0}, Ljava/util/TreeSet;-><init>()V

    return-object p0

    nop

    :pswitch_data_0
    .packed-switch 0x1
        :pswitch_7
        :pswitch_6
        :pswitch_5
        :pswitch_4
        :pswitch_3
        :pswitch_2
        :pswitch_1
        :pswitch_0
    .end packed-switch
.end method
