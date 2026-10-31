.class public final synthetic Lz/b;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Lz/d$a;


# instance fields
.field public final synthetic a:Landroid/view/accessibility/AccessibilityNodeInfo;

.field public final synthetic b:I

.field public final synthetic c:I

.field public final synthetic d:J


# direct methods
.method public synthetic constructor <init>(Landroid/view/accessibility/AccessibilityNodeInfo;IIJ)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lz/b;->a:Landroid/view/accessibility/AccessibilityNodeInfo;

    iput p2, p0, Lz/b;->b:I

    iput p3, p0, Lz/b;->c:I

    iput-wide p4, p0, Lz/b;->d:J

    return-void
.end method


# virtual methods
.method public final a(Lorg/xmlpull/v1/XmlSerializer;)V
    .locals 6

    iget-object v0, p0, Lz/b;->a:Landroid/view/accessibility/AccessibilityNodeInfo;

    iget v3, p0, Lz/b;->c:I

    iget-wide v4, p0, Lz/b;->d:J

    iget v2, p0, Lz/b;->b:I

    move-object v1, p1

    invoke-static/range {v0 .. v5}, Lz/d;->b(Landroid/view/accessibility/AccessibilityNodeInfo;Lorg/xmlpull/v1/XmlSerializer;IIJ)V

    return-void
.end method
