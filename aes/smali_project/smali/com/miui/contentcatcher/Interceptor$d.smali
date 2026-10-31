.class public final Lcom/miui/contentcatcher/Interceptor$d;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/miui/contentcatcher/Interceptor;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x9
    name = "d"
.end annotation


# instance fields
.field public final a:Landroid/view/View;

.field public final b:Landroid/os/Bundle;


# direct methods
.method public constructor <init>(Landroid/view/View;Landroid/os/Bundle;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/miui/contentcatcher/Interceptor$d;->a:Landroid/view/View;

    iput-object p2, p0, Lcom/miui/contentcatcher/Interceptor$d;->b:Landroid/os/Bundle;

    return-void
.end method
