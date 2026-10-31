.class public Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;
.super Ljava/lang/Object;
.source "SourceFile"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x9
    name = "Catcher"
.end annotation

.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher$View;
    }
.end annotation


# instance fields
.field private activity:Ljava/lang/String;

.field private views:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher$View;",
            ">;"
        }
    .end annotation
.end field


# direct methods
.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public getActivity()Ljava/lang/String;
    .locals 0

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;->activity:Ljava/lang/String;

    return-object p0
.end method

.method public getViews()Ljava/util/List;
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/List<",
            "Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher$View;",
            ">;"
        }
    .end annotation

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;->views:Ljava/util/List;

    return-object p0
.end method

.method public setActivity(Ljava/lang/String;)V
    .locals 0

    iput-object p1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;->activity:Ljava/lang/String;

    return-void
.end method

.method public setViews(Ljava/util/List;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List<",
            "Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher$View;",
            ">;)V"
        }
    .end annotation

    iput-object p1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;->views:Ljava/util/List;

    return-void
.end method

.method public toString()Ljava/lang/String;
    .locals 2

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "Catcher{activity=\'"

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v1, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;->activity:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    const-string v1, "\', views="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    iget-object p0, p0, Lcom/miui/contentcatcher/service/config/ViewConfig$Package$Config$Catcher;->views:Ljava/util/List;

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    const/16 p0, 0x7d

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method
