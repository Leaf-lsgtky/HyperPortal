.class public final Ly/g$b;
.super Landroid/database/ContentObserver;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Ly/g;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = null
.end annotation


# instance fields
.field public final synthetic a:Ly/g;


# direct methods
.method public constructor <init>(Ly/g;Landroid/os/Handler;)V
    .locals 0

    iput-object p1, p0, Ly/g$b;->a:Ly/g;

    invoke-direct {p0, p2}, Landroid/database/ContentObserver;-><init>(Landroid/os/Handler;)V

    return-void
.end method


# virtual methods
.method public final onChange(ZLandroid/net/Uri;)V
    .locals 0

    invoke-super {p0, p1, p2}, Landroid/database/ContentObserver;->onChange(ZLandroid/net/Uri;)V

    iget-object p0, p0, Ly/g$b;->a:Ly/g;

    invoke-virtual {p0, p2}, Ly/g;->d(Landroid/net/Uri;)V

    return-void
.end method
