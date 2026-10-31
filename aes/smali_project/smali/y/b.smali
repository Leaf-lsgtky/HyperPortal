.class public final Ly/b;
.super LB/c;
.source "SourceFile"


# instance fields
.field public final synthetic a:Ljava/util/HashMap;


# direct methods
.method public constructor <init>(Ljava/util/HashMap;)V
    .locals 0

    iput-object p1, p0, Ly/b;->a:Ljava/util/HashMap;

    invoke-direct {p0}, Landroid/os/Binder;-><init>()V

    const-string p1, "com.xiaomi.aicr.IGlobalCollectCatcherInterface"

    invoke-virtual {p0, p0, p1}, Landroid/os/Binder;->attachInterface(Landroid/os/IInterface;Ljava/lang/String;)V

    return-void
.end method
