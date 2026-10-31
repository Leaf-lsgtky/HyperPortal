.class public final Lh/d;
.super Ljava/lang/Object;
.source "SourceFile"


# static fields
.field public static final a:Z

.field public static final b:Lh/a$a;

.field public static final c:Lh/b$a;

.field public static final d:Lh/c$a;


# direct methods
.method static constructor <clinit>()V
    .locals 1

    :try_start_0
    const-string v0, "java.sql.Date"

    invoke-static {v0}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;
    :try_end_0
    .catch Ljava/lang/ClassNotFoundException; {:try_start_0 .. :try_end_0} :catch_0

    const/4 v0, 0x1

    goto :goto_0

    :catch_0
    const/4 v0, 0x0

    :goto_0
    sput-boolean v0, Lh/d;->a:Z

    if-eqz v0, :cond_0

    sget-object v0, Lh/a;->b:Lh/a$a;

    sput-object v0, Lh/d;->b:Lh/a$a;

    sget-object v0, Lh/b;->b:Lh/b$a;

    sput-object v0, Lh/d;->c:Lh/b$a;

    sget-object v0, Lh/c;->b:Lh/c$a;

    sput-object v0, Lh/d;->d:Lh/c$a;

    goto :goto_1

    :cond_0
    const/4 v0, 0x0

    sput-object v0, Lh/d;->b:Lh/a$a;

    sput-object v0, Lh/d;->c:Lh/b$a;

    sput-object v0, Lh/d;->d:Lh/c$a;

    :goto_1
    return-void
.end method
