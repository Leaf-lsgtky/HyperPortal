.class public final synthetic Ls/g;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Landroid/webkit/ValueCallback;


# instance fields
.field public final synthetic a:I

.field public final synthetic b:Landroid/webkit/ValueCallback;


# direct methods
.method public synthetic constructor <init>(Landroid/webkit/ValueCallback;I)V
    .locals 0

    iput p2, p0, Ls/g;->a:I

    iput-object p1, p0, Ls/g;->b:Landroid/webkit/ValueCallback;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public final onReceiveValue(Ljava/lang/Object;)V
    .locals 2

    iget v0, p0, Ls/g;->a:I

    check-cast p1, Ljava/lang/String;

    packed-switch v0, :pswitch_data_0

    const-string v0, "WebViewWrapperBase"

    const-string v1, "fetchWebViewUrl get result"

    invoke-static {v0, v1}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p0, p0, Ls/g;->b:Landroid/webkit/ValueCallback;

    check-cast p0, Lm/b;

    invoke-virtual {p0, p1}, Lm/b;->onReceiveValue(Ljava/lang/Object;)V

    return-void

    :pswitch_0
    const-string v0, "WebViewWrapperBase"

    const-string v1, "fetchWebViewText get result"

    invoke-static {v0, v1}, Lp/b;->a(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p0, p0, Ls/g;->b:Landroid/webkit/ValueCallback;

    check-cast p0, Lcom/miui/contentcatcher/Interceptor$c;

    invoke-virtual {p0, p1}, Lcom/miui/contentcatcher/Interceptor$c;->onReceiveValue(Ljava/lang/Object;)V

    return-void

    nop

    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_0
    .end packed-switch
.end method
