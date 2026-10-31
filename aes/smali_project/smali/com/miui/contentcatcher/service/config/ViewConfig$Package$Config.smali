.class public Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/miui/contentcatcher/service/config/ViewConfig$Package;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x9
    name = "Config"
.end annotation

.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;
    }
.end annotation


# instance fields
.field private catchers:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;",
            ">;"
        }
    .end annotation
.end field

.field private version:I


# direct methods
.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static bridge synthetic a(Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;)I
    .locals 0

    iget p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;->version:I

    return p0
.end method


# virtual methods
.method public getCatcher(Ljava/lang/String;)Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;
    .locals 3

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;->catchers:Ljava/util/List;

    const/4 v0, 0x0

    if-nez p0, :cond_0

    return-object v0

    :cond_0
    invoke-interface {p0}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object p0

    :cond_1
    invoke-interface {p0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_2

    invoke-interface {p0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;

    invoke-virtual {v1}, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;->getActivity()Ljava/lang/String;

    move-result-object v2

    invoke-static {v2, p1}, Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z

    move-result v2

    if-eqz v2, :cond_1

    return-object v1

    :cond_2
    return-object v0
.end method

.method public getCatchers()Ljava/util/List;
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/List<",
            "Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;",
            ">;"
        }
    .end annotation

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;->catchers:Ljava/util/List;

    return-object p0
.end method

.method public getVersion()I
    .locals 0

    iget p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;->version:I

    return p0
.end method

.method public setCatchers(Ljava/util/List;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List<",
            "Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;",
            ">;)V"
        }
    .end annotation

    iput-object p1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;->catchers:Ljava/util/List;

    return-void
.end method

.method public setVersion(I)V
    .locals 0

    iput p1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;->version:I

    return-void
.end method

.method public toString()Ljava/lang/String;
    .locals 2

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "Config{version=\'"

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget v1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;->version:I

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v1, "\', catchers="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;->catchers:Ljava/util/List;

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const/16 p0, 0x7d

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method
