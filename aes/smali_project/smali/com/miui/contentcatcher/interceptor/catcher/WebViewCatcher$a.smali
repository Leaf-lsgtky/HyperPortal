.class public final Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$a;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/lang/Runnable;


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->m()V
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = null
.end annotation


# instance fields
.field public final synthetic a:Landroid/view/View;

.field public final synthetic b:Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;


# direct methods
.method public constructor <init>(Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;Landroid/view/View;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()V"
        }
    .end annotation

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$a;->b:Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;

    iput-object p2, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$a;->a:Landroid/view/View;

    return-void
.end method


# virtual methods
.method public final run()V
    .locals 1

    iget-object v0, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$a;->b:Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;

    iget-object p0, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$a;->a:Landroid/view/View;

    invoke-virtual {v0, p0}, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->o(Landroid/view/View;)V

    return-void
.end method
