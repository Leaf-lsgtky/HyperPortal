.class public final Lz/j;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lz/j$c;,
        Lz/j$b;,
        Lz/j$a;
    }
.end annotation


# static fields
.field public static volatile d:Lz/j$b;


# instance fields
.field public final a:Landroid/os/Handler;

.field public volatile b:Z

.field public final c:Ljava/util/ArrayDeque;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/ArrayDeque<",
            "Lz/j$c;",
            ">;"
        }
    .end annotation
.end field


# direct methods
.method static constructor <clinit>()V
    .locals 1

    sget-object v0, Lz/j$b;->a:Lz/j$b;

    sput-object v0, Lz/j;->d:Lz/j$b;

    return-void
.end method

.method public constructor <init>(Landroid/os/Looper;)V
    .locals 2

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const/4 v0, 0x0

    iput-boolean v0, p0, Lz/j;->b:Z

    new-instance v0, Ljava/util/ArrayDeque;

    const/4 v1, 0x5

    invoke-direct {v0, v1}, Ljava/util/ArrayDeque;-><init>(I)V

    iput-object v0, p0, Lz/j;->c:Ljava/util/ArrayDeque;

    new-instance v0, Landroid/os/Handler;

    new-instance v1, Lz/e;

    invoke-direct {v1, p0}, Lz/e;-><init>(Lz/j;)V

    invoke-direct {v0, p1, v1}, Landroid/os/Handler;-><init>(Landroid/os/Looper;Landroid/os/Handler$Callback;)V

    iput-object v0, p0, Lz/j;->a:Landroid/os/Handler;

    return-void
.end method

.method public static a(Landroid/view/View;)Landroid/view/accessibility/AccessibilityNodeInfo;
    .locals 6

    const-string v0, "Catcher_AICollectorManager"

    invoke-virtual {p0}, Landroid/view/View;->getHandler()Landroid/os/Handler;

    move-result-object v1

    const/4 v2, 0x0

    if-nez v1, :cond_0

    return-object v2

    :cond_0
    invoke-static {}, Landroid/os/Looper;->myLooper()Landroid/os/Looper;

    move-result-object v3

    invoke-virtual {v1}, Landroid/os/Handler;->getLooper()Landroid/os/Looper;

    move-result-object v4

    if-ne v3, v4, :cond_1

    invoke-virtual {p0}, Landroid/view/View;->createAccessibilityNodeInfo()Landroid/view/accessibility/AccessibilityNodeInfo;

    move-result-object p0

    return-object p0

    :cond_1
    new-instance v3, Ljava/util/concurrent/FutureTask;

    new-instance v4, Lz/i;

    invoke-direct {v4, p0}, Lz/i;-><init>(Landroid/view/View;)V

    invoke-direct {v3, v4}, Ljava/util/concurrent/FutureTask;-><init>(Ljava/util/concurrent/Callable;)V

    invoke-virtual {v1, v3}, Landroid/os/Handler;->post(Ljava/lang/Runnable;)Z

    move-result p0

    if-nez p0, :cond_2

    return-object v2

    :cond_2
    const/4 p0, 0x0

    :try_start_0
    sget-object v1, Ljava/util/concurrent/TimeUnit;->MILLISECONDS:Ljava/util/concurrent/TimeUnit;

    const-wide/16 v4, 0x3e8

    invoke-virtual {v3, v4, v5, v1}, Ljava/util/concurrent/FutureTask;->get(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/view/accessibility/AccessibilityNodeInfo;
    :try_end_0
    .catch Ljava/lang/InterruptedException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    return-object v1

    :catch_0
    move-exception v1

    goto :goto_0

    :catch_1
    move-exception v1

    goto :goto_1

    :goto_0
    invoke-virtual {v3, p0}, Ljava/util/concurrent/FutureTask;->cancel(Z)Z

    new-instance p0, Ljava/lang/StringBuilder;

    const-string v3, "create accessibility node on UI thread failed: "

    invoke-direct {p0, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v0, p0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    return-object v2

    :goto_1
    invoke-static {}, Ljava/lang/Thread;->currentThread()Ljava/lang/Thread;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Thread;->interrupt()V

    invoke-virtual {v3, p0}, Ljava/util/concurrent/FutureTask;->cancel(Z)Z

    new-instance p0, Ljava/lang/StringBuilder;

    const-string v3, "create accessibility node on UI thread interrupted: "

    invoke-direct {p0, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v0, p0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    return-object v2
.end method

.method public static c(Lz/j$c;)V
    .locals 11

    const-string v0, "Catcher_AICollectorManager"

    const-string v1, "findNodes"

    invoke-static {v0, v1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v1, p0, Lz/j$c;->b:Ljava/lang/ref/WeakReference;

    invoke-virtual {v1}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/view/View;

    if-nez v1, :cond_0

    iget-object v0, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v1, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p0, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v2, "activity destroyed"

    invoke-static {v2, v0, v1, p0}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return-void

    :cond_0
    iget-object v2, p0, Lz/j$c;->d:Landroid/os/Bundle;

    const-string v3, "search_text"

    invoke-static {v2, v3}, Lz/k;->f(Landroid/os/Bundle;Ljava/lang/String;)Ljava/util/ArrayList;

    move-result-object v2

    iget-object v4, p0, Lz/j$c;->d:Landroid/os/Bundle;

    const-string v5, "search_view_id"

    invoke-static {v4, v5}, Lz/k;->f(Landroid/os/Bundle;Ljava/lang/String;)Ljava/util/ArrayList;

    move-result-object v4

    iget-object v6, p0, Lz/j$c;->d:Landroid/os/Bundle;

    const-string v7, "include_focus"

    invoke-static {v6, v7}, Lz/k;->f(Landroid/os/Bundle;Ljava/lang/String;)Ljava/util/ArrayList;

    move-result-object v6

    if-eqz v2, :cond_1

    new-instance v8, Ljava/lang/StringBuilder;

    invoke-direct {v8, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v8, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v0, v3}, Lp/b;->j(Ljava/lang/String;Ljava/lang/String;)V

    :cond_1
    if-eqz v4, :cond_2

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v0, v3}, Lp/b;->j(Ljava/lang/String;Ljava/lang/String;)V

    :cond_2
    if-eqz v6, :cond_3

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v0, v3}, Lp/b;->j(Ljava/lang/String;Ljava/lang/String;)V

    :cond_3
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v7

    invoke-static {v1}, Lz/j;->a(Landroid/view/View;)Landroid/view/accessibility/AccessibilityNodeInfo;

    move-result-object v3

    new-instance v5, Ljava/lang/StringBuilder;

    const-string v9, "createAccessibilityNodeInfoOnUiThread time: "

    invoke-direct {v5, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v9

    sub-long/2addr v9, v7

    invoke-virtual {v5, v9, v10}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-static {v0, v5}, Lp/b;->j(Ljava/lang/String;Ljava/lang/String;)V

    if-eqz v3, :cond_9

    const/4 v5, 0x1

    invoke-virtual {v3, v1, v5}, Landroid/view/accessibility/AccessibilityNodeInfo;->setQueryFromAppProcessEnabled(Landroid/view/View;Z)V

    const/4 v7, 0x0

    :try_start_0
    const-string v8, "Catcher find nodes"

    invoke-static {v8}, Landroid/os/Trace;->beginSection(Ljava/lang/String;)V

    new-instance v8, Ljava/util/ArrayList;

    invoke-direct {v8}, Ljava/util/ArrayList;-><init>()V

    if-eqz v2, :cond_4

    invoke-virtual {v2}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v2

    :goto_0
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    move-result v9

    if-eqz v9, :cond_4

    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v9

    check-cast v9, Ljava/lang/String;

    invoke-virtual {v3, v9}, Landroid/view/accessibility/AccessibilityNodeInfo;->findAccessibilityNodeInfosByText(Ljava/lang/String;)Ljava/util/List;

    move-result-object v9

    invoke-virtual {v8, v9}, Ljava/util/ArrayList;->addAll(Ljava/util/Collection;)Z

    goto :goto_0

    :catchall_0
    move-exception v0

    goto/16 :goto_3

    :cond_4
    if-eqz v4, :cond_5

    invoke-virtual {v4}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v2

    :goto_1
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    move-result v4

    if-eqz v4, :cond_5

    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/lang/String;

    invoke-virtual {v3, v4}, Landroid/view/accessibility/AccessibilityNodeInfo;->findAccessibilityNodeInfosByViewId(Ljava/lang/String;)Ljava/util/List;

    move-result-object v4

    invoke-virtual {v8, v4}, Ljava/util/ArrayList;->addAll(Ljava/util/Collection;)Z

    goto :goto_1

    :cond_5
    if-eqz v6, :cond_8

    new-instance v2, Ljava/util/HashSet;

    invoke-direct {v2, v6}, Ljava/util/HashSet;-><init>(Ljava/util/Collection;)V

    invoke-virtual {v2}, Ljava/util/HashSet;->iterator()Ljava/util/Iterator;

    move-result-object v2

    :cond_6
    :goto_2
    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z

    move-result v4

    if-eqz v4, :cond_8

    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/lang/String;

    const-string v6, "FOCUS_INPUT"

    invoke-virtual {v6, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v6

    if-eqz v6, :cond_7

    invoke-virtual {v3, v5}, Landroid/view/accessibility/AccessibilityNodeInfo;->findFocus(I)Landroid/view/accessibility/AccessibilityNodeInfo;

    move-result-object v4

    if-eqz v4, :cond_6

    invoke-virtual {v8, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_2

    :cond_7
    const-string v6, "FOCUS_ACCESSIBILITY"

    invoke-virtual {v6, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_6

    const/4 v4, 0x2

    invoke-virtual {v3, v4}, Landroid/view/accessibility/AccessibilityNodeInfo;->findFocus(I)Landroid/view/accessibility/AccessibilityNodeInfo;

    move-result-object v4

    if-eqz v4, :cond_6

    invoke-virtual {v8, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_2

    :cond_8
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "findNodes size "

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v8}, Ljava/util/ArrayList;->size()I

    move-result v4

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v2}, Lp/b;->j(Ljava/lang/String;Ljava/lang/String;)V

    iget v0, p0, Lz/j$c;->h:I

    iget-object v2, p0, Lz/j$c;->i:Landroid/graphics/Rect;

    invoke-virtual {v2}, Landroid/graphics/Rect;->width()I

    move-result v2

    iget-object v4, p0, Lz/j$c;->i:Landroid/graphics/Rect;

    invoke-virtual {v4}, Landroid/graphics/Rect;->height()I

    move-result v4

    new-instance v5, Lz/a;

    invoke-direct {v5, v8, v2, v4}, Lz/a;-><init>(Ljava/util/ArrayList;II)V

    invoke-static {v5, v0}, Lz/d;->f(Lz/d$a;I)Ljava/lang/String;

    move-result-object v0

    iget-object v2, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v4, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget v5, p0, Lz/j$c;->g:I

    iget-object v6, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v2, v4, v0, v5, v6}, Lz/k;->c(Ljava/lang/String;Landroid/os/Bundle;Ljava/lang/String;ILmiui/security/IUIAgentCallback;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    invoke-static {}, Landroid/os/Trace;->endSection()V

    invoke-virtual {v3, v1, v7}, Landroid/view/accessibility/AccessibilityNodeInfo;->setQueryFromAppProcessEnabled(Landroid/view/View;Z)V

    return-void

    :goto_3
    :try_start_1
    invoke-virtual {v0}, Ljava/lang/Throwable;->printStackTrace()V

    const-string v0, "findNodes failed"

    iget-object v2, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v4, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p0, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v0, v2, v4, p0}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    invoke-static {}, Landroid/os/Trace;->endSection()V

    invoke-virtual {v3, v1, v7}, Landroid/view/accessibility/AccessibilityNodeInfo;->setQueryFromAppProcessEnabled(Landroid/view/View;Z)V

    return-void

    :catchall_1
    move-exception p0

    invoke-static {}, Landroid/os/Trace;->endSection()V

    invoke-virtual {v3, v1, v7}, Landroid/view/accessibility/AccessibilityNodeInfo;->setQueryFromAppProcessEnabled(Landroid/view/View;Z)V

    throw p0

    :cond_9
    iget-object v0, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v1, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p0, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v2, "root accessibilityNodeInfo is null"

    invoke-static {v2, v0, v1, p0}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return-void
.end method

.method public static e(Landroid/view/View;Ljava/util/ArrayList;Lz/j$a;Ljava/util/List;I)Z
    .locals 8

    invoke-virtual {p0}, Landroid/view/View;->getContentDescription()Ljava/lang/CharSequence;

    move-result-object v0

    const/4 v1, 0x1

    const-string v2, "Catcher_AICollectorManager"

    const/4 v3, 0x0

    if-eqz v0, :cond_1

    invoke-interface {v0}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;

    move-result-object v0

    invoke-interface {p3}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v4

    :cond_0
    invoke-interface {v4}, Ljava/util/Iterator;->hasNext()Z

    move-result v5

    if-eqz v5, :cond_1

    invoke-interface {v4}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Ljava/lang/String;

    invoke-virtual {v0, v5}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v5

    if-eqz v5, :cond_0

    const-string v4, "contentDescription matched "

    invoke-virtual {v4, v0}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    invoke-static {v2, v0}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    move v0, v1

    goto :goto_0

    :cond_1
    move v0, v3

    :goto_0
    if-nez v0, :cond_5

    instance-of v4, p0, Landroid/widget/TextView;

    if-eqz v4, :cond_5

    move-object v4, p0

    check-cast v4, Landroid/widget/TextView;

    invoke-virtual {v4}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;

    move-result-object v5

    if-eqz v5, :cond_3

    invoke-interface {v5}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;

    move-result-object v5

    invoke-interface {p3}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v6

    :cond_2
    invoke-interface {v6}, Ljava/util/Iterator;->hasNext()Z

    move-result v7

    if-eqz v7, :cond_3

    invoke-interface {v6}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v7

    check-cast v7, Ljava/lang/String;

    invoke-virtual {v5, v7}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v7

    if-eqz v7, :cond_2

    const-string v0, "text matched "

    invoke-virtual {v0, v5}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    invoke-static {v2, v0}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    move v0, v1

    :cond_3
    if-nez v0, :cond_5

    invoke-virtual {v4}, Landroid/widget/TextView;->getHint()Ljava/lang/CharSequence;

    move-result-object v4

    if-eqz v4, :cond_5

    invoke-interface {v4}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;

    move-result-object v4

    invoke-interface {p3}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v5

    :cond_4
    invoke-interface {v5}, Ljava/util/Iterator;->hasNext()Z

    move-result v6

    if-eqz v6, :cond_5

    invoke-interface {v5}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v6

    check-cast v6, Ljava/lang/String;

    invoke-virtual {v4, v6}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v6

    if-eqz v6, :cond_4

    const-string v0, "hint matched "

    invoke-virtual {v0, v4}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    invoke-static {v2, v0}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    move v0, v1

    :cond_5
    if-eqz v0, :cond_6

    new-instance v0, Lz/l;

    invoke-direct {v0}, Lz/l;-><init>()V

    iget-object v4, p2, Lz/j$a;->a:Landroid/graphics/Matrix;

    iget-object v5, p2, Lz/j$a;->b:Landroid/graphics/Rect;

    invoke-virtual {v0, p0, v4, v5}, Lz/l;->c(Landroid/view/View;Landroid/graphics/Matrix;Landroid/graphics/Rect;)V

    invoke-virtual {p1, v0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_6
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v4

    iget-wide v6, p2, Lz/j$a;->c:J

    sub-long/2addr v4, v6

    iget v0, p2, Lz/j$a;->e:I

    int-to-long v6, v0

    cmp-long v0, v4, v6

    if-lez v0, :cond_7

    new-instance p0, Ljava/lang/StringBuilder;

    const-string p1, "trigger MAX_PROCESS_TIME "

    invoke-direct {p0, p1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget p1, p2, Lz/j$a;->e:I

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string p1, "ms"

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v2, p0}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    return v3

    :cond_7
    instance-of v0, p0, Landroid/view/ViewGroup;

    if-eqz v0, :cond_9

    check-cast p0, Landroid/view/ViewGroup;

    iget v0, p2, Lz/j$a;->d:I

    if-ge p4, v0, :cond_9

    move v0, v3

    :goto_1
    invoke-virtual {p0}, Landroid/view/ViewGroup;->getChildCount()I

    move-result v2

    if-ge v0, v2, :cond_9

    invoke-virtual {p0, v0}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v2

    invoke-virtual {v2}, Landroid/view/View;->getAlpha()F

    move-result v4

    const/4 v5, 0x0

    cmpl-float v4, v4, v5

    if-lez v4, :cond_8

    invoke-virtual {v2}, Landroid/view/View;->getTransitionAlpha()F

    move-result v4

    cmpl-float v4, v4, v5

    if-lez v4, :cond_8

    invoke-virtual {v2}, Landroid/view/View;->getVisibility()I

    move-result v4

    if-nez v4, :cond_8

    add-int/lit8 v4, p4, 0x1

    invoke-static {v2, p1, p2, p3, v4}, Lz/j;->e(Landroid/view/View;Ljava/util/ArrayList;Lz/j$a;Ljava/util/List;I)Z

    move-result v2

    if-nez v2, :cond_8

    return v3

    :cond_8
    add-int/lit8 v0, v0, 0x1

    goto :goto_1

    :cond_9
    return v1
.end method

.method public static f(Lz/j$c;)V
    .locals 9

    const-string v0, "Catcher_AICollectorManager"

    const-string v1, "injectText"

    invoke-static {v0, v1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v0, p0, Lz/j$c;->b:Ljava/lang/ref/WeakReference;

    invoke-virtual {v0}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/view/View;

    if-nez v0, :cond_0

    iget-object v0, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v1, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p0, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v2, "activity destroyed"

    invoke-static {v2, v0, v1, p0}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return-void

    :cond_0
    invoke-virtual {v0}, Landroid/view/View;->createAccessibilityNodeInfo()Landroid/view/accessibility/AccessibilityNodeInfo;

    move-result-object v1

    if-eqz v1, :cond_3

    const/4 v2, 0x1

    invoke-virtual {v1, v0, v2}, Landroid/view/accessibility/AccessibilityNodeInfo;->setQueryFromAppProcessEnabled(Landroid/view/View;Z)V

    const/4 v3, 0x0

    :try_start_0
    invoke-virtual {v1, v2}, Landroid/view/accessibility/AccessibilityNodeInfo;->findFocus(I)Landroid/view/accessibility/AccessibilityNodeInfo;

    move-result-object v2

    if-eqz v2, :cond_2

    new-instance v4, Landroid/os/Bundle;

    invoke-direct {v4}, Landroid/os/Bundle;-><init>()V

    const-string v5, "ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE"

    iget-object v6, p0, Lz/j$c;->d:Landroid/os/Bundle;

    const-string v7, "text"

    const-string v8, ""

    invoke-virtual {v6, v7, v8}, Landroid/os/Bundle;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v6

    invoke-virtual {v4, v5, v6}, Landroid/os/Bundle;->putCharSequence(Ljava/lang/String;Ljava/lang/CharSequence;)V

    invoke-virtual {v2}, Landroid/view/accessibility/AccessibilityNodeInfo;->getActionList()Ljava/util/List;

    move-result-object v5

    if-eqz v5, :cond_1

    sget-object v6, Landroid/view/accessibility/AccessibilityNodeInfo$AccessibilityAction;->ACTION_SET_TEXT:Landroid/view/accessibility/AccessibilityNodeInfo$AccessibilityAction;

    invoke-interface {v5, v6}, Ljava/util/List;->contains(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_1

    const/high16 v5, 0x200000

    invoke-virtual {v2, v5, v4}, Landroid/view/accessibility/AccessibilityNodeInfo;->performAction(ILandroid/os/Bundle;)Z

    iget-object v2, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v4, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget v5, p0, Lz/j$c;->g:I

    iget-object v6, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v2, v4, v5, v6}, Lz/k;->b(Ljava/lang/String;Landroid/os/Bundle;ILmiui/security/IUIAgentCallback;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    invoke-virtual {v1, v0, v3}, Landroid/view/accessibility/AccessibilityNodeInfo;->setQueryFromAppProcessEnabled(Landroid/view/View;Z)V

    return-void

    :cond_1
    :try_start_1
    const-string v2, "Not support action"

    iget-object v4, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v5, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object v6, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v2, v4, v5, v6}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    invoke-virtual {v1, v0, v3}, Landroid/view/accessibility/AccessibilityNodeInfo;->setQueryFromAppProcessEnabled(Landroid/view/View;Z)V

    return-void

    :cond_2
    :try_start_2
    const-string v2, "no focus input node"

    iget-object v4, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v5, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object v6, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v2, v4, v5, v6}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    invoke-virtual {v1, v0, v3}, Landroid/view/accessibility/AccessibilityNodeInfo;->setQueryFromAppProcessEnabled(Landroid/view/View;Z)V

    return-void

    :catchall_0
    :try_start_3
    const-string v2, "Inject failed"

    iget-object v4, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v5, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p0, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v2, v4, v5, p0}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_1

    invoke-virtual {v1, v0, v3}, Landroid/view/accessibility/AccessibilityNodeInfo;->setQueryFromAppProcessEnabled(Landroid/view/View;Z)V

    return-void

    :catchall_1
    move-exception p0

    invoke-virtual {v1, v0, v3}, Landroid/view/accessibility/AccessibilityNodeInfo;->setQueryFromAppProcessEnabled(Landroid/view/View;Z)V

    throw p0

    :cond_3
    iget-object v0, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v1, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p0, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v2, "root accessibilityNodeInfo is null"

    invoke-static {v2, v0, v1, p0}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return-void
.end method

.method public static g(Landroid/app/Activity;Landroid/view/View;Landroid/os/Bundle;)Ljava/lang/String;
    .locals 2

    if-nez p1, :cond_0

    const-string p0, "activity/window/decorView is null"

    return-object p0

    :cond_0
    const-string v0, "TargetActivity"

    const-string v1, ""

    invoke-virtual {p2, v0, v1}, Landroid/os/Bundle;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    if-nez p0, :cond_1

    goto :goto_0

    :cond_1
    if-eqz v0, :cond_4

    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v1

    if-nez v1, :cond_4

    invoke-virtual {p0}, Landroid/app/Activity;->getComponentName()Landroid/content/ComponentName;

    move-result-object v1

    if-eqz v1, :cond_3

    invoke-virtual {p0}, Landroid/app/Activity;->getComponentName()Landroid/content/ComponentName;

    move-result-object p0

    invoke-virtual {p0}, Landroid/content/ComponentName;->getClassName()Ljava/lang/String;

    move-result-object p0

    invoke-static {p0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v1

    if-nez v1, :cond_2

    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v1

    if-nez v1, :cond_4

    invoke-virtual {v0, p0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v1

    if-nez v1, :cond_4

    :cond_2
    new-instance p1, Ljava/lang/StringBuilder;

    invoke-direct {p1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string p0, " != "

    invoke-virtual {p1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    const-string p1, "Catcher_AICollectorManager"

    invoke-static {p1, p0}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    :cond_3
    :goto_0
    const-string p0, "activity not match"

    return-object p0

    :cond_4
    invoke-virtual {p1}, Landroid/view/View;->getDisplay()Landroid/view/Display;

    move-result-object p0

    if-nez p0, :cond_5

    const-string p0, "display is null"

    return-object p0

    :cond_5
    const-string p1, "displayId"

    const/4 v0, -0x1

    invoke-static {p2, p1, v0}, Lz/k;->e(Landroid/os/Bundle;Ljava/lang/String;I)I

    move-result p1

    if-eq p1, v0, :cond_6

    invoke-virtual {p0}, Landroid/view/Display;->getDisplayId()I

    move-result p0

    if-eq p0, p1, :cond_6

    const-string p0, "display_id not match"

    return-object p0

    :cond_6
    const/4 p0, 0x0

    return-object p0
.end method

.method public static j(Lz/j$c;)V
    .locals 13

    const-string v0, "startUIAgent onResult error throw:"

    const-string v1, "Catcher_AICollectorManager"

    const-string v2, "startXMLRequest"

    invoke-static {v1, v2}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v2, p0, Lz/j$c;->b:Ljava/lang/ref/WeakReference;

    invoke-virtual {v2}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Landroid/view/View;

    if-nez v2, :cond_0

    iget-object v0, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v1, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p0, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v2, "activity destroyed"

    invoke-static {v2, v0, v1, p0}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return-void

    :cond_0
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v3

    invoke-static {v2}, Lz/j;->a(Landroid/view/View;)Landroid/view/accessibility/AccessibilityNodeInfo;

    move-result-object v11

    new-instance v5, Ljava/lang/StringBuilder;

    const-string v6, "createAccessibilityNodeInfoOnUiThread time: "

    invoke-direct {v5, v6}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v6

    sub-long/2addr v6, v3

    invoke-virtual {v5, v6, v7}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v1, v3}, Lp/b;->j(Ljava/lang/String;Ljava/lang/String;)V

    if-nez v11, :cond_1

    iget-object v0, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v1, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p0, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v2, "root accessibilityNodeInfo is null"

    invoke-static {v2, v0, v1, p0}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return-void

    :cond_1
    const/4 v3, 0x0

    :try_start_0
    const-string v4, "Catcher XMLRequest"

    invoke-static {v4}, Landroid/os/Trace;->beginSection(Ljava/lang/String;)V

    const/4 v4, 0x1

    invoke-virtual {v11, v2, v4}, Landroid/view/accessibility/AccessibilityNodeInfo;->setQueryFromAppProcessEnabled(Landroid/view/View;Z)V

    iget-object v4, p0, Lz/j$c;->d:Landroid/os/Bundle;

    const-string v5, "max_time_limit"

    const/16 v6, 0x1f4

    invoke-static {v4, v5, v6}, Lz/k;->e(Landroid/os/Bundle;Ljava/lang/String;I)I

    move-result v4

    const/16 v5, 0x3e8

    invoke-static {v4, v5}, Ljava/lang/Math;->min(II)I

    move-result v4

    invoke-static {v3, v4}, Ljava/lang/Math;->max(II)I

    move-result v4

    iget v12, p0, Lz/j$c;->h:I

    iget-object v5, p0, Lz/j$c;->i:Landroid/graphics/Rect;

    invoke-virtual {v5}, Landroid/graphics/Rect;->width()I

    move-result v7

    iget-object v5, p0, Lz/j$c;->i:Landroid/graphics/Rect;

    invoke-virtual {v5}, Landroid/graphics/Rect;->height()I

    move-result v8

    sget-object v5, Lz/d;->a:[Ljava/lang/String;

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v5

    int-to-long v9, v4

    add-long/2addr v9, v5

    new-instance v4, Lz/b;

    move-object v5, v4

    move-object v6, v11

    invoke-direct/range {v5 .. v10}, Lz/b;-><init>(Landroid/view/accessibility/AccessibilityNodeInfo;IIJ)V

    invoke-static {v4, v12}, Lz/d;->f(Lz/d$a;I)Ljava/lang/String;

    move-result-object v4

    iget-object v5, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v6, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget v7, p0, Lz/j$c;->g:I

    iget-object v8, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v5, v6, v4, v7, v8}, Lz/k;->c(Ljava/lang/String;Landroid/os/Bundle;Ljava/lang/String;ILmiui/security/IUIAgentCallback;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    :goto_0
    invoke-virtual {v11, v2, v3}, Landroid/view/accessibility/AccessibilityNodeInfo;->setQueryFromAppProcessEnabled(Landroid/view/View;Z)V

    invoke-static {}, Landroid/os/Trace;->endSection()V

    goto :goto_1

    :catchall_0
    move-exception v4

    :try_start_1
    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v5, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v1, v0}, Lp/b;->e(Ljava/lang/String;Ljava/lang/String;)V

    const-string v0, "capture error"

    iget-object v1, p0, Lz/j$c;->a:Ljava/lang/String;

    iget-object v4, p0, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p0, p0, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {v0, v1, v4, p0}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    goto :goto_0

    :goto_1
    return-void

    :catchall_1
    move-exception p0

    invoke-virtual {v11, v2, v3}, Landroid/view/accessibility/AccessibilityNodeInfo;->setQueryFromAppProcessEnabled(Landroid/view/View;Z)V

    invoke-static {}, Landroid/os/Trace;->endSection()V

    throw p0
.end method

.method public static k(Landroid/view/View;Lz/l;Lz/j$a;I)Z
    .locals 6

    iget-object v0, p2, Lz/j$a;->a:Landroid/graphics/Matrix;

    iget-object v1, p2, Lz/j$a;->b:Landroid/graphics/Rect;

    invoke-virtual {p1, p0, v0, v1}, Lz/l;->c(Landroid/view/View;Landroid/graphics/Matrix;Landroid/graphics/Rect;)V

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v0

    iget-wide v2, p2, Lz/j$a;->c:J

    sub-long/2addr v0, v2

    iget v2, p2, Lz/j$a;->e:I

    int-to-long v2, v2

    cmp-long v0, v0, v2

    const/4 v1, 0x0

    if-lez v0, :cond_0

    new-instance p0, Ljava/lang/StringBuilder;

    const-string p1, "trigger MAX_PROCESS_TIME "

    invoke-direct {p0, p1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget p1, p2, Lz/j$a;->e:I

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string p1, "ms"

    invoke-virtual {p0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    const-string p1, "Catcher_AICollectorManager"

    invoke-static {p1, p0}, Lp/b;->m(Ljava/lang/String;Ljava/lang/String;)V

    return v1

    :cond_0
    instance-of v0, p0, Landroid/view/ViewGroup;

    const/4 v2, 0x1

    if-eqz v0, :cond_2

    check-cast p0, Landroid/view/ViewGroup;

    iget v0, p2, Lz/j$a;->d:I

    if-ge p3, v0, :cond_2

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    iput-object v0, p1, Lz/l;->a:Ljava/util/ArrayList;

    move v0, v1

    :goto_0
    invoke-virtual {p0}, Landroid/view/ViewGroup;->getChildCount()I

    move-result v3

    if-ge v0, v3, :cond_2

    invoke-virtual {p0, v0}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v3

    invoke-virtual {v3}, Landroid/view/View;->getAlpha()F

    move-result v4

    const/4 v5, 0x0

    cmpl-float v4, v4, v5

    if-lez v4, :cond_1

    invoke-virtual {v3}, Landroid/view/View;->getTransitionAlpha()F

    move-result v4

    cmpl-float v4, v4, v5

    if-lez v4, :cond_1

    invoke-virtual {v3}, Landroid/view/View;->getVisibility()I

    move-result v4

    if-nez v4, :cond_1

    new-instance v4, Lz/l;

    invoke-direct {v4}, Lz/l;-><init>()V

    iget-object v5, p1, Lz/l;->a:Ljava/util/ArrayList;

    invoke-virtual {v5, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    add-int/lit8 v5, p3, 0x1

    invoke-static {v3, v4, p2, v5}, Lz/j;->k(Landroid/view/View;Lz/l;Lz/j$a;I)Z

    move-result v3

    if-nez v3, :cond_1

    return v1

    :cond_1
    add-int/lit8 v0, v0, 0x1

    goto :goto_0

    :cond_2
    return v2
.end method


# virtual methods
.method public final b(Lz/j$c;)Z
    .locals 5

    const-string v0, "Catcher_AICollectorManager"

    const-string v1, "dumpViews"

    invoke-static {v0, v1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v0, p1, Lz/j$c;->b:Ljava/lang/ref/WeakReference;

    invoke-virtual {v0}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/view/View;

    iget-object v1, p1, Lz/j$c;->c:Ljava/lang/ref/WeakReference;

    invoke-virtual {v1}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/app/Activity;

    const/4 v2, 0x1

    if-eqz v0, :cond_2

    if-nez v1, :cond_0

    goto :goto_0

    :cond_0
    invoke-virtual {v1}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    move-result-object v1

    invoke-virtual {v1}, Landroid/view/Window;->getAttributes()Landroid/view/WindowManager$LayoutParams;

    move-result-object v1

    iget v1, v1, Landroid/view/WindowManager$LayoutParams;->flags:I

    and-int/lit16 v1, v1, 0x2000

    if-eqz v1, :cond_1

    iget-object p0, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v0, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p1, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v1, "Not allowed when window set FLAG_SECURE"

    invoke-static {v1, p0, v0, p1}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return v2

    :cond_1
    iget-object v1, p0, Lz/j;->a:Landroid/os/Handler;

    const/4 v2, 0x2

    invoke-virtual {v1, v2}, Landroid/os/Handler;->obtainMessage(I)Landroid/os/Message;

    move-result-object v2

    iput-object p1, v2, Landroid/os/Message;->obj:Ljava/lang/Object;

    const-wide/16 v3, 0x3e8

    invoke-virtual {v1, v2, v3, v4}, Landroid/os/Handler;->sendMessageDelayed(Landroid/os/Message;J)Z

    new-instance v1, Lz/f;

    invoke-direct {v1, p0, v0, p1}, Lz/f;-><init>(Lz/j;Landroid/view/View;Lz/j$c;)V

    invoke-virtual {v0, v1}, Landroid/view/View;->post(Ljava/lang/Runnable;)Z

    const/4 p0, 0x0

    return p0

    :cond_2
    :goto_0
    iget-object p0, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v0, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p1, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v1, "Activity destroyed"

    invoke-static {v1, p0, v0, p1}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return v2
.end method

.method public final d(Lz/j$c;)Z
    .locals 6

    const-string v0, "Catcher_AICollectorManager"

    const-string v1, "findNodesBySelf"

    invoke-static {v0, v1}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v1, p1, Lz/j$c;->b:Ljava/lang/ref/WeakReference;

    invoke-virtual {v1}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/view/View;

    iget-object v2, p1, Lz/j$c;->c:Ljava/lang/ref/WeakReference;

    invoke-virtual {v2}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Landroid/app/Activity;

    const/4 v3, 0x1

    if-eqz v1, :cond_4

    if-nez v2, :cond_0

    goto :goto_1

    :cond_0
    invoke-virtual {v2}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    move-result-object v2

    invoke-virtual {v2}, Landroid/view/Window;->getAttributes()Landroid/view/WindowManager$LayoutParams;

    move-result-object v2

    iget v2, v2, Landroid/view/WindowManager$LayoutParams;->flags:I

    and-int/lit16 v2, v2, 0x2000

    if-eqz v2, :cond_1

    iget-object p0, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v0, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p1, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v1, "Not allowed when window set FLAG_SECURE"

    invoke-static {v1, p0, v0, p1}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return v3

    :cond_1
    iget-object v2, p1, Lz/j$c;->d:Landroid/os/Bundle;

    const-string v4, "search_text"

    invoke-static {v2, v4}, Lz/k;->f(Landroid/os/Bundle;Ljava/lang/String;)Ljava/util/ArrayList;

    move-result-object v2

    if-eqz v2, :cond_3

    invoke-virtual {v2}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v5

    if-eqz v5, :cond_2

    goto :goto_0

    :cond_2
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v0, v3}, Lp/b;->j(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v0, p0, Lz/j;->a:Landroid/os/Handler;

    const/4 v3, 0x2

    invoke-virtual {v0, v3}, Landroid/os/Handler;->obtainMessage(I)Landroid/os/Message;

    move-result-object v3

    iput-object p1, v3, Landroid/os/Message;->obj:Ljava/lang/Object;

    const-wide/16 v4, 0x3e8

    invoke-virtual {v0, v3, v4, v5}, Landroid/os/Handler;->sendMessageDelayed(Landroid/os/Message;J)Z

    new-instance v0, Lz/g;

    invoke-direct {v0, p0, v1, p1, v2}, Lz/g;-><init>(Lz/j;Landroid/view/View;Lz/j$c;Ljava/util/ArrayList;)V

    invoke-virtual {v1, v0}, Landroid/view/View;->post(Ljava/lang/Runnable;)Z

    const/4 p0, 0x0

    return p0

    :cond_3
    :goto_0
    iget-object p0, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v0, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p1, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v1, "search_text empty"

    invoke-static {v1, p0, v0, p1}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return v3

    :cond_4
    :goto_1
    iget-object p0, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v0, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p1, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v1, "Activity destroyed"

    invoke-static {v1, p0, v0, p1}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return v3
.end method

.method public final h(Lz/j$c;)Z
    .locals 6

    const-string v0, "not support uiAgentType "

    const-string v1, "not support uiAgent way "

    const/4 v2, 0x1

    :try_start_0
    iget-object v3, p1, Lz/j$c;->d:Landroid/os/Bundle;

    const-string v4, "uiAgentType"

    const/4 v5, -0x1

    invoke-static {v3, v4, v5}, Lz/k;->e(Landroid/os/Bundle;Ljava/lang/String;I)I

    move-result v3

    if-nez v3, :cond_5

    iget-object v0, p1, Lz/j$c;->d:Landroid/os/Bundle;

    const-string v3, "catch_way"

    invoke-static {v0, v3, v5}, Lz/k;->e(Landroid/os/Bundle;Ljava/lang/String;I)I

    move-result v0

    if-ne v0, v5, :cond_0

    invoke-virtual {p0, p1}, Lz/j;->i(Lz/j$c;)Z

    move-result p0

    return p0

    :catch_0
    move-exception p0

    goto :goto_0

    :cond_0
    if-nez v0, :cond_1

    invoke-static {p1}, Lz/j;->j(Lz/j$c;)V

    return v2

    :cond_1
    if-ne v0, v2, :cond_2

    invoke-virtual {p0, p1}, Lz/j;->b(Lz/j$c;)Z

    move-result p0

    return p0

    :cond_2
    const/4 v3, 0x2

    if-ne v0, v3, :cond_3

    invoke-static {p1}, Lz/j;->c(Lz/j$c;)V

    return v2

    :cond_3
    const/4 v3, 0x3

    if-ne v0, v3, :cond_4

    invoke-virtual {p0, p1}, Lz/j;->d(Lz/j$c;)Z

    move-result p0

    return p0

    :cond_4
    new-instance p0, Ljava/lang/StringBuilder;

    invoke-direct {p0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    iget-object v0, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v1, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object v3, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {p0, v0, v1, v3}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return v2

    :cond_5
    if-ne v3, v2, :cond_6

    invoke-static {p1}, Lz/j;->f(Lz/j$c;)V

    return v2

    :cond_6
    new-instance p0, Ljava/lang/StringBuilder;

    invoke-direct {p0, v0}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    iget-object v0, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v1, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object v3, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {p0, v0, v1, v3}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    return v2

    :goto_0
    invoke-virtual {p0}, Ljava/lang/Throwable;->printStackTrace()V

    iget-object p0, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v0, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p1, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    const-string v1, "Exception in parse UIAgent "

    invoke-static {v1, p0, v0, p1}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return v2
.end method

.method public final i(Lz/j$c;)Z
    .locals 4

    const-string v0, "startUIAgent"

    const-string v1, "Catcher_AICollectorManager"

    invoke-static {v1, v0}, Lp/b;->f(Ljava/lang/String;Ljava/lang/String;)V

    const/4 v0, 0x1

    :try_start_0
    invoke-static {}, Lcom/xiaomi/aicollector/AICollectorController;->getInstance()Lcom/xiaomi/aicollector/AICollectorController;

    move-result-object v1

    new-instance v2, Lz/h;

    invoke-direct {v2, p0, p1}, Lz/h;-><init>(Lz/j;Lz/j$c;)V

    invoke-interface {v1, v2}, Lcom/xiaomi/aicollector/AICollectorController;->buildXMLRequest(Lcom/xiaomi/aicollector/AICollectorRequest$AICollectorRequestListener;)Lcom/xiaomi/aicollector/AICollectorRequest;

    move-result-object p0

    if-eqz p0, :cond_0

    iget-object v1, p1, Lz/j$c;->f:Ljava/lang/String;

    invoke-virtual {p0, v1}, Lcom/xiaomi/aicollector/AICollectorRequest;->setRequestName(Ljava/lang/String;)V

    iget-object v1, p1, Lz/j$c;->d:Landroid/os/Bundle;

    invoke-virtual {p0, v1}, Lcom/xiaomi/aicollector/AICollectorRequest;->setExtraData(Landroid/os/Bundle;)V

    invoke-static {}, Lcom/xiaomi/aicollector/AICollectorController;->getInstance()Lcom/xiaomi/aicollector/AICollectorController;

    move-result-object v1

    invoke-interface {v1, p0}, Lcom/xiaomi/aicollector/AICollectorController;->pushRequest(Lcom/xiaomi/aicollector/AICollectorRequest;)Z

    const/4 p0, 0x0

    return p0

    :catchall_0
    move-exception p0

    goto :goto_0

    :cond_0
    const-string p0, "request is empty"

    iget-object v1, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v2, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object v3, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {p0, v1, v2, v3}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    return v0

    :goto_0
    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "startUIAgent error: "

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Lp/b;->k(Ljava/lang/String;)V

    invoke-virtual {p0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;

    move-result-object p0

    iget-object v1, p1, Lz/j$c;->a:Ljava/lang/String;

    iget-object v2, p1, Lz/j$c;->d:Landroid/os/Bundle;

    iget-object p1, p1, Lz/j$c;->e:Lmiui/security/IUIAgentCallback;

    invoke-static {p0, v1, v2, p1}, Lz/k;->a(Ljava/lang/String;Ljava/lang/String;Landroid/os/Bundle;Lmiui/security/IUIAgentCallback;)V

    return v0
.end method
