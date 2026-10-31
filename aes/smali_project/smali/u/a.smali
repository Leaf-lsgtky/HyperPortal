.class public final Lu/a;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lu/a$a;
    }
.end annotation


# instance fields
.field public final a:[I

.field public final b:[F

.field public final c:[F

.field public final d:F

.field public final e:I

.field public f:I

.field public final g:Lu/a$a;

.field public final h:Lcom/miui/contentcatcher/Interceptor;

.field public i:Z

.field public j:Z

.field public k:Landroid/graphics/PointF;

.field public l:Landroid/graphics/PointF;

.field public m:Landroid/graphics/PointF;

.field public n:Landroid/graphics/PointF;

.field public final o:Landroid/app/Activity;

.field public p:Ljava/lang/ref/WeakReference;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/lang/ref/WeakReference<",
            "Landroid/view/View;",
            ">;"
        }
    .end annotation
.end field

.field public q:Landroid/view/View;

.field public r:Z

.field public final s:Z

.field public final t:Z

.field public u:Z

.field public v:Z

.field public w:Z


# direct methods
.method public constructor <init>(Landroid/content/Context;Lcom/miui/contentcatcher/Interceptor;Landroid/app/Activity;ZZZ)V
    .locals 3

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const/4 v0, 0x2

    new-array v1, v0, [I

    iput-object v1, p0, Lu/a;->a:[I

    new-array v1, v0, [F

    iput-object v1, p0, Lu/a;->b:[F

    new-array v1, v0, [F

    iput-object v1, p0, Lu/a;->c:[F

    iput v0, p0, Lu/a;->f:I

    const/4 v0, 0x0

    iput-object v0, p0, Lu/a;->k:Landroid/graphics/PointF;

    iput-object v0, p0, Lu/a;->l:Landroid/graphics/PointF;

    new-instance v1, Landroid/graphics/PointF;

    const/4 v2, 0x0

    invoke-direct {v1, v2, v2}, Landroid/graphics/PointF;-><init>(FF)V

    iput-object v1, p0, Lu/a;->m:Landroid/graphics/PointF;

    iput-object v0, p0, Lu/a;->n:Landroid/graphics/PointF;

    const/4 v0, 0x0

    iput-boolean v0, p0, Lu/a;->v:Z

    iput-boolean v0, p0, Lu/a;->w:Z

    iput-object p2, p0, Lu/a;->h:Lcom/miui/contentcatcher/Interceptor;

    iput-object p3, p0, Lu/a;->o:Landroid/app/Activity;

    new-instance p2, Lu/a$a;

    invoke-direct {p2, p0}, Lu/a$a;-><init>(Lu/a;)V

    iput-object p2, p0, Lu/a;->g:Lu/a$a;

    invoke-static {p1}, Landroid/view/ViewConfiguration;->get(Landroid/content/Context;)Landroid/view/ViewConfiguration;

    move-result-object p2

    invoke-virtual {p2}, Landroid/view/ViewConfiguration;->getScaledTouchSlop()I

    move-result p2

    int-to-float p2, p2

    iput p2, p0, Lu/a;->d:F

    invoke-virtual {p1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object p1

    invoke-static {}, Landroid/view/ViewConfiguration;->getLongPressTimeout()I

    move-result p2

    add-int/lit8 p2, p2, 0x32

    const-string p3, "support_long_press_time_out"

    invoke-static {p1, p3, p2}, Landroid/provider/Settings$System;->getInt(Landroid/content/ContentResolver;Ljava/lang/String;I)I

    move-result p1

    iput p1, p0, Lu/a;->e:I

    iput-boolean p4, p0, Lu/a;->r:Z

    iput-boolean p5, p0, Lu/a;->s:Z

    iput-boolean p6, p0, Lu/a;->t:Z

    return-void
.end method


# virtual methods
.method public final a()V
    .locals 6

    const/4 v0, 0x0

    iput-boolean v0, p0, Lu/a;->w:Z

    iget-boolean v1, p0, Lu/a;->r:Z

    iget-object v2, p0, Lu/a;->h:Lcom/miui/contentcatcher/Interceptor;

    const/4 v3, 0x0

    if-eqz v1, :cond_1

    if-eqz v2, :cond_1

    iget-object v1, p0, Lu/a;->g:Lu/a$a;

    const/4 v4, 0x1

    invoke-virtual {v1, v4}, Landroid/os/Handler;->hasMessages(I)Z

    move-result v5

    if-eqz v5, :cond_0

    invoke-virtual {v1, v4}, Landroid/os/Handler;->removeMessages(I)V

    :cond_0
    iput-object v3, p0, Lu/a;->q:Landroid/view/View;

    iput-boolean v0, p0, Lu/a;->j:Z

    :cond_1
    if-eqz v2, :cond_2

    iget-object v0, p0, Lu/a;->p:Ljava/lang/ref/WeakReference;

    invoke-virtual {v0}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Landroid/view/View;

    :cond_2
    iput-object v3, p0, Lu/a;->k:Landroid/graphics/PointF;

    new-instance v0, Landroid/graphics/PointF;

    const/4 v1, 0x0

    invoke-direct {v0, v1, v1}, Landroid/graphics/PointF;-><init>(FF)V

    iput-object v0, p0, Lu/a;->m:Landroid/graphics/PointF;

    return-void
.end method

.method public final b(Landroid/view/KeyEvent;Landroid/view/View;)V
    .locals 6

    iget-boolean v0, p0, Lu/a;->s:Z

    if-nez v0, :cond_0

    return-void

    :cond_0
    invoke-virtual {p1}, Landroid/view/KeyEvent;->getKeyCode()I

    move-result v0

    invoke-virtual {p1}, Landroid/view/KeyEvent;->getRepeatCount()I

    move-result v1

    invoke-virtual {p1}, Landroid/view/KeyEvent;->getAction()I

    move-result v2

    const/4 v3, 0x0

    const/4 v4, 0x1

    if-nez v2, :cond_1

    move v2, v4

    goto :goto_0

    :cond_1
    move v2, v3

    :goto_0
    invoke-virtual {p1}, Landroid/view/KeyEvent;->isCanceled()Z

    move-result p1

    const/4 v5, 0x4

    if-ne v0, v5, :cond_4

    if-eqz v2, :cond_2

    if-nez v1, :cond_4

    iput-boolean v4, p0, Lu/a;->v:Z

    goto :goto_1

    :cond_2
    const-string v0, "Interceptor_Catcher"

    iget-object v1, p0, Lu/a;->h:Lcom/miui/contentcatcher/Interceptor;

    if-nez p1, :cond_3

    iget-boolean p1, p0, Lu/a;->v:Z

    if-eqz p1, :cond_4

    iput-boolean v3, p0, Lu/a;->v:Z

    if-eqz v1, :cond_4

    new-instance p0, Ljava/lang/StringBuilder;

    const-string p1, "rootView onBackPressed: "

    invoke-direct {p0, p1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v0, p0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p0, v1, Lcom/miui/contentcatcher/Interceptor;->y:Landroid/os/Handler;

    const/16 p1, 0x65

    invoke-virtual {p0, p1}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    return-void

    :cond_3
    iget-boolean p1, p0, Lu/a;->v:Z

    if-eqz p1, :cond_4

    iput-boolean v3, p0, Lu/a;->v:Z

    if-eqz v1, :cond_4

    new-instance p0, Ljava/lang/StringBuilder;

    const-string p1, "rootView onBackCancel: "

    invoke-direct {p0, p1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {p0, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {v0, p0}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    iget-object p0, v1, Lcom/miui/contentcatcher/Interceptor;->y:Landroid/os/Handler;

    const/16 p1, 0x69

    invoke-virtual {p0, p1}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    :cond_4
    :goto_1
    return-void
.end method

.method public final c(Landroid/view/MotionEvent;Landroid/view/View;)V
    .locals 20

    move-object/from16 v0, p0

    move-object/from16 v1, p1

    move-object/from16 v2, p2

    iget-object v3, v0, Lu/a;->p:Ljava/lang/ref/WeakReference;

    if-eqz v3, :cond_0

    invoke-virtual {v3}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v3

    if-eq v2, v3, :cond_1

    :cond_0
    new-instance v3, Ljava/lang/ref/WeakReference;

    invoke-direct {v3, v2}, Ljava/lang/ref/WeakReference;-><init>(Ljava/lang/Object;)V

    iput-object v3, v0, Lu/a;->p:Ljava/lang/ref/WeakReference;

    const/4 v3, 0x0

    iput-object v3, v0, Lu/a;->k:Landroid/graphics/PointF;

    :cond_1
    invoke-virtual/range {p1 .. p1}, Landroid/view/MotionEvent;->getActionMasked()I

    move-result v3

    iget-object v4, v0, Lu/a;->g:Lu/a$a;

    iget v5, v0, Lu/a;->e:I

    iget-object v6, v0, Lu/a;->h:Lcom/miui/contentcatcher/Interceptor;

    const/4 v7, 0x1

    iget-object v8, v0, Lu/a;->o:Landroid/app/Activity;

    if-eqz v3, :cond_16

    iget-boolean v10, v0, Lu/a;->t:Z

    const-string v11, "onLongPress catched content, so cancel this event:"

    const-string v13, "CatcherInputEventListener"

    if-eq v3, v7, :cond_13

    iget-object v14, v0, Lu/a;->c:[F

    iget-object v15, v0, Lu/a;->b:[F

    iget-object v9, v0, Lu/a;->a:[I

    const-string v7, "Move over TouchSlop so dismiss Content exetions"

    const-string v12, "Interceptor_Catcher"

    move-object/from16 v17, v7

    const/4 v7, 0x2

    if-eq v3, v7, :cond_9

    const/4 v7, 0x3

    if-eq v3, v7, :cond_8

    const/4 v7, 0x5

    if-eq v3, v7, :cond_4

    const/4 v2, 0x6

    if-eq v3, v2, :cond_2

    goto/16 :goto_4

    :cond_2
    iget-boolean v2, v0, Lu/a;->i:Z

    if-eqz v2, :cond_3

    iget-boolean v2, v0, Lu/a;->j:Z

    if-eqz v2, :cond_3

    if-eqz v10, :cond_3

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2, v11}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v13, v2}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    const/4 v2, 0x3

    invoke-virtual {v1, v2}, Landroid/view/MotionEvent;->setAction(I)V

    :cond_3
    invoke-virtual/range {p0 .. p0}, Lu/a;->a()V

    goto/16 :goto_4

    :cond_4
    iget v3, v0, Lu/a;->f:I

    const/4 v7, 0x2

    if-ne v3, v7, :cond_6

    invoke-virtual/range {p1 .. p1}, Landroid/view/MotionEvent;->getPointerCount()I

    move-result v3

    if-ne v3, v7, :cond_6

    invoke-virtual/range {p1 .. p1}, Landroid/view/MotionEvent;->getEventTime()J

    move-result-wide v10

    invoke-virtual/range {p1 .. p1}, Landroid/view/MotionEvent;->getDownTime()J

    move-result-wide v18

    sub-long v10, v10, v18

    const-wide/16 v18, 0xc8

    cmp-long v3, v10, v18

    if-gez v3, :cond_6

    iget-boolean v3, v0, Lu/a;->r:Z

    if-eqz v3, :cond_17

    iget-object v3, v0, Lu/a;->k:Landroid/graphics/PointF;

    if-eqz v3, :cond_17

    invoke-virtual {v8}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    move-result-object v3

    invoke-virtual {v3}, Landroid/view/Window;->getDecorView()Landroid/view/View;

    move-result-object v3

    if-ne v2, v3, :cond_17

    const/4 v2, 0x0

    :goto_0
    const/4 v3, 0x2

    if-ge v2, v3, :cond_5

    invoke-virtual {v1, v2}, Landroid/view/MotionEvent;->getPointerId(I)I

    move-result v3

    aput v3, v9, v2

    invoke-virtual {v1, v2}, Landroid/view/MotionEvent;->getRawY(I)F

    move-result v3

    aput v3, v15, v2

    invoke-virtual {v1, v2}, Landroid/view/MotionEvent;->getRawX(I)F

    move-result v3

    aput v3, v14, v2

    add-int/lit8 v2, v2, 0x1

    goto :goto_0

    :cond_5
    new-instance v2, Landroid/graphics/PointF;

    const/4 v3, 0x1

    invoke-virtual {v1, v3}, Landroid/view/MotionEvent;->getRawX(I)F

    move-result v6

    invoke-virtual {v1, v3}, Landroid/view/MotionEvent;->getRawY(I)F

    move-result v1

    invoke-direct {v2, v6, v1}, Landroid/graphics/PointF;-><init>(FF)V

    iput-object v2, v0, Lu/a;->m:Landroid/graphics/PointF;

    iput-object v2, v0, Lu/a;->n:Landroid/graphics/PointF;

    iget-object v1, v0, Lu/a;->k:Landroid/graphics/PointF;

    iget-object v2, v0, Lu/a;->p:Ljava/lang/ref/WeakReference;

    invoke-virtual {v2}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Landroid/view/View;

    iput-object v2, v0, Lu/a;->q:Landroid/view/View;

    invoke-static {}, Landroid/os/Message;->obtain()Landroid/os/Message;

    move-result-object v0

    iput-object v1, v0, Landroid/os/Message;->obj:Ljava/lang/Object;

    const/4 v1, 0x1

    iput v1, v0, Landroid/os/Message;->what:I

    int-to-long v1, v5

    invoke-virtual {v4, v0, v1, v2}, Landroid/os/Handler;->sendMessageDelayed(Landroid/os/Message;J)Z

    goto/16 :goto_4

    :cond_6
    iget-boolean v1, v0, Lu/a;->u:Z

    if-nez v1, :cond_7

    if-eqz v6, :cond_7

    iget-boolean v1, v0, Lu/a;->i:Z

    if-eqz v1, :cond_7

    iget-boolean v1, v0, Lu/a;->j:Z

    if-eqz v1, :cond_7

    move-object/from16 v2, v17

    invoke-static {v12, v2}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v1, v6, Lcom/miui/contentcatcher/Interceptor;->y:Landroid/os/Handler;

    const/16 v2, 0x68

    invoke-virtual {v1, v2}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    const/4 v1, 0x1

    iput-boolean v1, v0, Lu/a;->u:Z

    goto/16 :goto_4

    :cond_7
    invoke-virtual/range {p0 .. p0}, Lu/a;->a()V

    goto/16 :goto_4

    :cond_8
    invoke-virtual/range {p0 .. p0}, Lu/a;->a()V

    goto/16 :goto_4

    :cond_9
    move-object/from16 v2, v17

    iget v3, v0, Lu/a;->f:I

    iget v4, v0, Lu/a;->d:F

    const/4 v5, 0x1

    if-ne v3, v5, :cond_b

    iget-object v3, v0, Lu/a;->k:Landroid/graphics/PointF;

    if-eqz v3, :cond_a

    invoke-virtual/range {p1 .. p1}, Landroid/view/MotionEvent;->getRawX()F

    move-result v3

    invoke-virtual/range {p1 .. p1}, Landroid/view/MotionEvent;->getRawY()F

    move-result v5

    iget-object v7, v0, Lu/a;->k:Landroid/graphics/PointF;

    iget v8, v7, Landroid/graphics/PointF;->x:F

    iget v7, v7, Landroid/graphics/PointF;->y:F

    sub-float/2addr v8, v3

    sub-float/2addr v7, v5

    float-to-double v10, v8

    float-to-double v7, v7

    invoke-static {v10, v11, v7, v8}, Ljava/lang/Math;->hypot(DD)D

    move-result-wide v7

    double-to-float v3, v7

    invoke-static {v3}, Ljava/lang/Math;->abs(F)F

    move-result v3

    cmpl-float v3, v3, v4

    if-lez v3, :cond_b

    invoke-virtual/range {p0 .. p0}, Lu/a;->a()V

    goto/16 :goto_4

    :cond_a
    iget-object v3, v0, Lu/a;->l:Landroid/graphics/PointF;

    if-eqz v3, :cond_b

    iget-boolean v3, v0, Lu/a;->u:Z

    if-nez v3, :cond_b

    if-eqz v6, :cond_b

    iget-boolean v3, v0, Lu/a;->i:Z

    if-eqz v3, :cond_b

    invoke-virtual/range {p1 .. p1}, Landroid/view/MotionEvent;->getRawX()F

    move-result v3

    invoke-virtual/range {p1 .. p1}, Landroid/view/MotionEvent;->getRawY()F

    move-result v5

    iget-object v7, v0, Lu/a;->l:Landroid/graphics/PointF;

    iget v8, v7, Landroid/graphics/PointF;->x:F

    iget v7, v7, Landroid/graphics/PointF;->y:F

    sub-float/2addr v8, v3

    sub-float/2addr v7, v5

    float-to-double v10, v8

    float-to-double v7, v7

    invoke-static {v10, v11, v7, v8}, Ljava/lang/Math;->hypot(DD)D

    move-result-wide v7

    double-to-float v3, v7

    invoke-static {v3}, Ljava/lang/Math;->abs(F)F

    move-result v3

    cmpl-float v3, v3, v4

    if-lez v3, :cond_b

    invoke-virtual {v6}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    invoke-static {v12, v2}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v1, v6, Lcom/miui/contentcatcher/Interceptor;->y:Landroid/os/Handler;

    const/16 v2, 0x68

    invoke-virtual {v1, v2}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    const/4 v1, 0x1

    iput-boolean v1, v0, Lu/a;->u:Z

    const-string v0, "Single onMoveOver catched content, so send message to content"

    invoke-static {v13, v0}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    goto/16 :goto_4

    :cond_b
    iget v3, v0, Lu/a;->f:I

    const/4 v5, 0x2

    if-ne v3, v5, :cond_17

    iget-object v3, v0, Lu/a;->k:Landroid/graphics/PointF;

    const/4 v5, 0x0

    if-eqz v3, :cond_f

    iget-object v3, v0, Lu/a;->m:Landroid/graphics/PointF;

    if-eqz v3, :cond_f

    iget v7, v3, Landroid/graphics/PointF;->x:F

    cmpl-float v7, v7, v5

    if-lez v7, :cond_f

    iget v3, v3, Landroid/graphics/PointF;->y:F

    cmpl-float v3, v3, v5

    if-lez v3, :cond_f

    move v2, v5

    const/4 v3, 0x0

    :goto_1
    const/4 v6, 0x2

    if-ge v3, v6, :cond_d

    aget v7, v9, v3

    invoke-virtual {v1, v7}, Landroid/view/MotionEvent;->findPointerIndex(I)I

    move-result v7

    if-ltz v7, :cond_17

    if-lt v7, v6, :cond_c

    goto/16 :goto_4

    :cond_c
    invoke-virtual {v1, v7}, Landroid/view/MotionEvent;->getRawY(I)F

    move-result v6

    aget v8, v15, v3

    sub-float/2addr v6, v8

    invoke-static {v6}, Ljava/lang/Math;->abs(F)F

    move-result v6

    add-float/2addr v2, v6

    invoke-virtual {v1, v7}, Landroid/view/MotionEvent;->getRawX(I)F

    move-result v6

    aget v7, v14, v3

    sub-float/2addr v6, v7

    invoke-static {v6}, Ljava/lang/Math;->abs(F)F

    move-result v6

    add-float/2addr v5, v6

    add-int/lit8 v3, v3, 0x1

    goto :goto_1

    :cond_d
    cmpl-float v1, v5, v4

    if-gtz v1, :cond_e

    cmpl-float v1, v2, v4

    if-lez v1, :cond_17

    :cond_e
    invoke-virtual/range {p0 .. p0}, Lu/a;->a()V

    goto/16 :goto_4

    :cond_f
    iget-object v3, v0, Lu/a;->l:Landroid/graphics/PointF;

    if-eqz v3, :cond_17

    iget-object v3, v0, Lu/a;->n:Landroid/graphics/PointF;

    if-eqz v3, :cond_17

    iget-boolean v3, v0, Lu/a;->u:Z

    if-nez v3, :cond_17

    if-eqz v6, :cond_17

    iget-boolean v3, v0, Lu/a;->i:Z

    if-eqz v3, :cond_17

    iget-boolean v3, v0, Lu/a;->r:Z

    if-eqz v3, :cond_17

    move v3, v5

    const/4 v7, 0x0

    const/4 v8, 0x2

    :goto_2
    if-ge v7, v8, :cond_11

    aget v10, v9, v7

    invoke-virtual {v1, v10}, Landroid/view/MotionEvent;->findPointerIndex(I)I

    move-result v10

    if-ltz v10, :cond_17

    if-lt v10, v8, :cond_10

    goto/16 :goto_4

    :cond_10
    invoke-virtual {v1, v10}, Landroid/view/MotionEvent;->getRawY(I)F

    move-result v11

    aget v16, v15, v7

    sub-float v11, v11, v16

    invoke-static {v11}, Ljava/lang/Math;->abs(F)F

    move-result v11

    add-float/2addr v3, v11

    invoke-virtual {v1, v10}, Landroid/view/MotionEvent;->getRawX(I)F

    move-result v10

    aget v11, v14, v7

    sub-float/2addr v10, v11

    invoke-static {v10}, Ljava/lang/Math;->abs(F)F

    move-result v10

    add-float/2addr v5, v10

    add-int/lit8 v7, v7, 0x1

    goto :goto_2

    :cond_11
    cmpl-float v1, v5, v4

    if-gtz v1, :cond_12

    cmpl-float v1, v3, v4

    if-lez v1, :cond_17

    :cond_12
    invoke-virtual {v6}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    invoke-static {v12, v2}, Lp/b;->g(Ljava/lang/String;Ljava/lang/String;)V

    iget-object v1, v6, Lcom/miui/contentcatcher/Interceptor;->y:Landroid/os/Handler;

    const/16 v2, 0x68

    invoke-virtual {v1, v2}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    const/4 v1, 0x1

    iput-boolean v1, v0, Lu/a;->u:Z

    const-string v0, "Double onMoveOver catched content, so send message to content"

    invoke-static {v13, v0}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    goto/16 :goto_4

    :cond_13
    iget-boolean v2, v0, Lu/a;->i:Z

    if-nez v2, :cond_14

    iget-object v3, v0, Lu/a;->k:Landroid/graphics/PointF;

    if-eqz v3, :cond_14

    goto :goto_3

    :cond_14
    if-eqz v2, :cond_15

    iget-boolean v2, v0, Lu/a;->j:Z

    if-eqz v2, :cond_15

    if-eqz v10, :cond_15

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2, v11}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v13, v2}, Lp/b;->d(Ljava/lang/String;Ljava/lang/String;)V

    const/4 v2, 0x3

    invoke-virtual {v1, v2}, Landroid/view/MotionEvent;->setAction(I)V

    :cond_15
    :goto_3
    invoke-virtual/range {p0 .. p0}, Lu/a;->a()V

    goto :goto_4

    :cond_16
    const/4 v3, 0x0

    iput-boolean v3, v0, Lu/a;->u:Z

    iput-boolean v3, v0, Lu/a;->i:Z

    iput-boolean v3, v0, Lu/a;->j:Z

    const/4 v3, 0x1

    iput-boolean v3, v0, Lu/a;->w:Z

    new-instance v3, Landroid/graphics/PointF;

    invoke-virtual/range {p1 .. p1}, Landroid/view/MotionEvent;->getRawX()F

    move-result v7

    invoke-virtual/range {p1 .. p1}, Landroid/view/MotionEvent;->getRawY()F

    move-result v1

    invoke-direct {v3, v7, v1}, Landroid/graphics/PointF;-><init>(FF)V

    iget-object v1, v0, Lu/a;->p:Ljava/lang/ref/WeakReference;

    invoke-virtual {v1}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/view/View;

    invoke-virtual {v6}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    iput-object v3, v0, Lu/a;->k:Landroid/graphics/PointF;

    iput-object v3, v0, Lu/a;->l:Landroid/graphics/PointF;

    iget v1, v0, Lu/a;->f:I

    const/4 v6, 0x1

    if-ne v1, v6, :cond_17

    iget-boolean v1, v0, Lu/a;->r:Z

    if-eqz v1, :cond_17

    invoke-virtual {v8}, Landroid/app/Activity;->getWindow()Landroid/view/Window;

    move-result-object v1

    invoke-virtual {v1}, Landroid/view/Window;->getDecorView()Landroid/view/View;

    move-result-object v1

    if-ne v2, v1, :cond_17

    iget-object v1, v0, Lu/a;->p:Ljava/lang/ref/WeakReference;

    invoke-virtual {v1}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/view/View;

    iput-object v1, v0, Lu/a;->q:Landroid/view/View;

    invoke-static {}, Landroid/os/Message;->obtain()Landroid/os/Message;

    move-result-object v0

    iput-object v3, v0, Landroid/os/Message;->obj:Ljava/lang/Object;

    const/4 v1, 0x1

    iput v1, v0, Landroid/os/Message;->what:I

    int-to-long v1, v5

    invoke-virtual {v4, v0, v1, v2}, Landroid/os/Handler;->sendMessageDelayed(Landroid/os/Message;J)Z

    :cond_17
    :goto_4
    return-void
.end method
