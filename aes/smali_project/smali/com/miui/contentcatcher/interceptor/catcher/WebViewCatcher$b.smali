.class public final Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$b;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Ljava/lang/Runnable;


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->h(Landroid/view/View;Z)V
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = null
.end annotation


# instance fields
.field public final synthetic a:Z

.field public final synthetic b:Landroid/view/View;

.field public final synthetic c:Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;


# direct methods
.method public constructor <init>(Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;ZLandroid/view/View;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()V"
        }
    .end annotation

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$b;->c:Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;

    iput-boolean p2, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$b;->a:Z

    iput-object p3, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$b;->b:Landroid/view/View;

    return-void
.end method


# virtual methods
.method public final run()V
    .locals 2

    iget-boolean v0, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$b;->a:Z

    iget-object v1, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$b;->b:Landroid/view/View;

    iget-object p0, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher$b;->c:Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;

    if-eqz v0, :cond_0

    invoke-virtual {p0, v1}, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->o(Landroid/view/View;)V

    goto :goto_0

    :cond_0
    invoke-static {v1}, Ljava/lang/System;->identityHashCode(Ljava/lang/Object;)I

    move-result v0

    invoke-static {v0}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v0

    iget-object p0, p0, Lcom/miui/contentcatcher/interceptor/catcher/WebViewCatcher;->l:Ljava/util/WeakHashMap;

    invoke-virtual {p0, v0}, Ljava/util/WeakHashMap;->remove(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p0

    check-cast p0, Ls/e;

    if-eqz p0, :cond_1

    iget-object v0, p0, Ls/e;->d:Ls/e$b;

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Landroid/os/Handler;->removeCallbacksAndMessages(Ljava/lang/Object;)V

    const/16 v0, 0xb

    iput v0, p0, Ls/e;->g:I

    :cond_1
    :goto_0
    return-void
.end method
