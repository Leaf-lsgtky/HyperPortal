.class public final Lcom/miui/contentcatcher/common/ExtraConfigInfo$a;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Landroid/os/Parcelable$Creator;


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/miui/contentcatcher/common/ExtraConfigInfo;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1
    name = null
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Object;",
        "Landroid/os/Parcelable$Creator<",
        "Lcom/miui/contentcatcher/common/ExtraConfigInfo;",
        ">;"
    }
.end annotation


# virtual methods
.method public final createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object;
    .locals 2

    new-instance p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    const-string v0, ""

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->a:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->b:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->c:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->d:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->e:Ljava/lang/String;

    const/4 v1, 0x0

    iput-boolean v1, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->f:Z

    iput-boolean v1, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->g:Z

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->h:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->i:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->j:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->k:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->l:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->m:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->n:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->o:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->p:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->q:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->r:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->s:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->t:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->u:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->v:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->w:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->x:Ljava/lang/String;

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->y:Ljava/lang/String;

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->z:Ljava/util/ArrayList;

    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->A:Ljava/util/ArrayList;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->a:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->b:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->c:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->d:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->h:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->i:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->j:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->k:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->l:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->m:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->n:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->o:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->p:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->q:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->r:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->s:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->t:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->u:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->v:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->w:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->x:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->y:Ljava/lang/String;

    invoke-static {}, Ljava/lang/ClassLoader;->getSystemClassLoader()Ljava/lang/ClassLoader;

    move-result-object v0

    invoke-virtual {p1, v0}, Landroid/os/Parcel;->readArrayList(Ljava/lang/ClassLoader;)Ljava/util/ArrayList;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->z:Ljava/util/ArrayList;

    invoke-static {}, Ljava/lang/ClassLoader;->getSystemClassLoader()Ljava/lang/ClassLoader;

    move-result-object v0

    invoke-virtual {p1, v0}, Landroid/os/Parcel;->readArrayList(Ljava/lang/ClassLoader;)Ljava/util/ArrayList;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->A:Ljava/util/ArrayList;

    invoke-virtual {p1}, Landroid/os/Parcel;->readString()Ljava/lang/String;

    move-result-object v0

    iput-object v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->e:Ljava/lang/String;

    invoke-virtual {p1}, Landroid/os/Parcel;->readBoolean()Z

    move-result v0

    iput-boolean v0, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->f:Z

    invoke-virtual {p1}, Landroid/os/Parcel;->readBoolean()Z

    move-result p1

    iput-boolean p1, p0, Lcom/miui/contentcatcher/common/ExtraConfigInfo;->g:Z

    return-object p0
.end method

.method public final newArray(I)[Ljava/lang/Object;
    .locals 0

    new-array p0, p1, [Lcom/miui/contentcatcher/common/ExtraConfigInfo;

    return-object p0
.end method
