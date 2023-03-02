; ModuleID = 'llvm-link'
source_filename = "llvm-link"
target datalayout = "e-m:e-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

@interesting = dso_local global i32 1
@MOD = dso_local constant i32 100005
@SUM = dso_local constant i32 777777
@.str = private unnamed_addr constant [3 x i8] c"%d\00", align 1
@.str.1 = private unnamed_addr constant [3 x i8] c"%c\00", align 1
@.str.2 = private unnamed_addr constant [4 x i8] c"%d:\00", align 1
@.str.3 = private unnamed_addr constant [4 x i8] c" %d\00", align 1
@.str.4 = private unnamed_addr constant [2 x i8] c"\0A\00", align 1

define dso_local i32 @one() {
  %1 = alloca i32
  store i32 1, i32* %1
  br label %2

; <label>:2:                                      ; preds = %0
  %3 = load i32, i32* %1
  ret i32 %3
}

define dso_local i32 @one2(i32) {
  %2 = alloca i32
  %3 = alloca i32
  store i32 %0, i32* %3
  %4 = load i32, i32* %3
  %5 = load i32, i32* %3
  %6 = mul i32 2, %5
  %7 = add i32 %6, 1
  %8 = icmp sgt i32 %4, %7
  br i1 %8, label %9, label %11

; <label>:9:                                      ; preds = %1
  %10 = load i32, i32* %3
  store i32 %10, i32* %2
  br label %19

; <label>:11:                                     ; preds = %1
  %12 = call i32 @one()
  %13 = call i32 @one()
  %14 = call i32 @one()
  %15 = sdiv i32 %13, %14
  %16 = add i32 %12, %15
  %17 = call i32 @one()
  %18 = sub i32 %16, %17
  store i32 %18, i32* %2
  br label %19

; <label>:19:                                     ; preds = %11, %9
  %20 = load i32, i32* %2
  ret i32 %20
}

define dso_local i32 @tRue() {
  %1 = alloca i32
  %2 = alloca i32
  store i32 -99, i32* %2
  br label %3

; <label>:3:                                      ; preds = %12, %0
  %4 = load i32, i32* %2
  %5 = call i32 @one()
  %6 = icmp slt i32 %4, %5
  br i1 %6, label %7, label %10

; <label>:7:                                      ; preds = %3
  %8 = load i32, i32* %2
  %9 = add i32 %8, 1
  store i32 %9, i32* %2
  br label %12

; <label>:10:                                     ; preds = %3
  %11 = load i32, i32* %2
  store i32 %11, i32* %1
  br label %42

; <label>:12:                                     ; preds = %7
  br label %3

; <label>:13:                                     ; preds = %22
  %14 = load i32, i32* %2
  %15 = call i32 @one()
  %16 = call i32 @one2(i32 %15)
  %17 = icmp slt i32 %14, %16
  br i1 %17, label %18, label %21

; <label>:18:                                     ; preds = %13
  %19 = load i32, i32* %2
  %20 = add i32 %19, 1
  store i32 %20, i32* %2
  br label %22

; <label>:21:                                     ; preds = %13
  br label %23

; <label>:22:                                     ; preds = %18
  br label %13

; <label>:23:                                     ; preds = %21
  br label %24

; <label>:24:                                     ; preds = %39, %23
  %25 = load i32, i32* %2
  %26 = load i32, i32* %2
  %27 = call i32 @one2(i32 %26)
  %28 = add i32 %25, %27
  %29 = call i32 @one()
  %30 = add i32 %29, 1
  %31 = call i32 @one2(i32 %30)
  %32 = call i32 @one2(i32 %31)
  %33 = icmp slt i32 %28, %32
  br i1 %33, label %34, label %37

; <label>:34:                                     ; preds = %24
  %35 = load i32, i32* %2
  %36 = add i32 %35, 1
  store i32 %36, i32* %2
  br label %39

; <label>:37:                                     ; preds = %24
  %38 = load i32, i32* %2
  store i32 %38, i32* %1
  br label %42

; <label>:39:                                     ; preds = %34
  br label %24
                                                  ; No predecessors!
  %41 = load i32, i32* %2
  store i32 %41, i32* %1
  br label %42

; <label>:42:                                     ; preds = %40, %37, %10
  %43 = load i32, i32* %1
  ret i32 %43
}

define dso_local i32 @fAlse() {
  %1 = alloca i32
  %2 = alloca i32
  %3 = call i32 @tRue()
  store i32 %3, i32* %2
  br label %4

; <label>:4:                                      ; preds = %14, %0
  %5 = load i32, i32* %2
  %6 = call i32 @tRue()
  %7 = call i32 @tRue()
  %8 = mul i32 %6, %7
  %9 = add i32 %5, %8
  store i32 %9, i32* %2
  %10 = load i32, i32* %2
  call void @putint(i32 %10)
  call void @putch(i32 10)
  %11 = load i32, i32* %2
  %12 = icmp sgt i32 %11, 48
  br i1 %12, label %13, label %14

; <label>:13:                                     ; preds = %4
  br label %15

; <label>:14:                                     ; preds = %4
  br label %4

; <label>:15:                                     ; preds = %13
  %16 = load i32, i32* %2
  %17 = call i32 @tRue()
  %18 = srem i32 %16, %17
  store i32 %18, i32* %1
  br label %19

; <label>:19:                                     ; preds = %15
  %20 = load i32, i32* %1
  ret i32 %20
}

define dso_local i32 @main() {
  %1 = alloca i32
  %2 = alloca i32
  %3 = call i32 @getint()
  store i32 %3, i32* %2
  br label %4

; <label>:4:                                      ; preds = %43, %0
  %5 = load i32, i32* %2
  %6 = srem i32 %5, 100005
  %7 = call i32 @tRue()
  %8 = call i32 @fAlse()
  %9 = add i32 %7, %8
  %10 = icmp slt i32 %6, %9
  br i1 %10, label %11, label %44

; <label>:11:                                     ; preds = %4
  %12 = call i32 @tRue()
  call void @putint(i32 %12)
  call void @putch(i32 10)
  %13 = load i32, i32* %2
  %14 = add i32 %13, 100
  store i32 %14, i32* %2
  br label %15

; <label>:15:                                     ; preds = %42, %11
  %16 = load i32, i32* %2
  %17 = sdiv i32 %16, 100005
  %18 = add i32 %17, 100005
  %19 = call i32 @tRue()
  %20 = call i32 @fAlse()
  %21 = add i32 %19, %20
  %22 = icmp slt i32 %18, %21
  br i1 %22, label %23, label %43

; <label>:23:                                     ; preds = %15
  %24 = load i32, i32* %2
  %25 = sub i32 %24, 1
  store i32 %25, i32* %2
  %26 = call i32 @tRue()
  %27 = load i32, i32* %2
  %28 = add i32 %26, %27
  call void @putint(i32 %28)
  call void @putch(i32 10)
  br label %29

; <label>:29:                                     ; preds = %36, %23
  %30 = load i32, i32* %2
  %31 = sdiv i32 %30, 100005
  %32 = call i32 @tRue()
  %33 = call i32 @fAlse()
  %34 = add i32 %32, %33
  %35 = icmp slt i32 %31, %34
  br i1 %35, label %36, label %42

; <label>:36:                                     ; preds = %29
  %37 = load i32, i32* %2
  %38 = sub i32 %37, 1
  store i32 %38, i32* %2
  %39 = call i32 @tRue()
  %40 = load i32, i32* %2
  %41 = add i32 %39, %40
  call void @putint(i32 %41)
  call void @putch(i32 10)
  br label %29

; <label>:42:                                     ; preds = %29
  br label %15

; <label>:43:                                     ; preds = %15
  br label %4

; <label>:44:                                     ; preds = %4
  %45 = load i32, i32* %2
  call void @putint(i32 %45)
  call void @putch(i32 10)
  store i32 0, i32* %1
  br label %46

; <label>:46:                                     ; preds = %44
  %47 = load i32, i32* %1
  ret i32 %47
}

; Function Attrs: noinline nounwind optnone uwtable
define i32 @getint() #0 {
  %1 = alloca i32, align 4
  %2 = call i32 (i8*, ...) @__isoc99_scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str, i32 0, i32 0), i32* %1)
  %3 = load i32, i32* %1, align 4
  ret i32 %3
}

declare i32 @__isoc99_scanf(i8*, ...) #1

; Function Attrs: noinline nounwind optnone uwtable
define i32 @getch() #0 {
  %1 = alloca i8, align 1
  %2 = call i32 (i8*, ...) @__isoc99_scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str.1, i32 0, i32 0), i8* %1)
  %3 = load i8, i8* %1, align 1
  %4 = sext i8 %3 to i32
  ret i32 %4
}

; Function Attrs: noinline nounwind optnone uwtable
define i32 @getarray(i32*) #0 {
  %2 = alloca i32*, align 8
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  store i32* %0, i32** %2, align 8
  %5 = call i32 (i8*, ...) @__isoc99_scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str, i32 0, i32 0), i32* %3)
  store i32 0, i32* %4, align 4
  br label %6

; <label>:6:                                      ; preds = %16, %1
  %7 = load i32, i32* %4, align 4
  %8 = load i32, i32* %3, align 4
  %9 = icmp slt i32 %7, %8
  br i1 %9, label %10, label %19

; <label>:10:                                     ; preds = %6
  %11 = load i32*, i32** %2, align 8
  %12 = load i32, i32* %4, align 4
  %13 = sext i32 %12 to i64
  %14 = getelementptr inbounds i32, i32* %11, i64 %13
  %15 = call i32 (i8*, ...) @__isoc99_scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str, i32 0, i32 0), i32* %14)
  br label %16

; <label>:16:                                     ; preds = %10
  %17 = load i32, i32* %4, align 4
  %18 = add nsw i32 %17, 1
  store i32 %18, i32* %4, align 4
  br label %6

; <label>:19:                                     ; preds = %6
  %20 = load i32, i32* %3, align 4
  ret i32 %20
}

; Function Attrs: noinline nounwind optnone uwtable
define void @putint(i32) #0 {
  %2 = alloca i32, align 4
  store i32 %0, i32* %2, align 4
  %3 = load i32, i32* %2, align 4
  %4 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str, i32 0, i32 0), i32 %3)
  ret void
}

declare i32 @printf(i8*, ...) #1

; Function Attrs: noinline nounwind optnone uwtable
define void @putch(i32) #0 {
  %2 = alloca i32, align 4
  store i32 %0, i32* %2, align 4
  %3 = load i32, i32* %2, align 4
  %4 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str.1, i32 0, i32 0), i32 %3)
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define void @putarray(i32, i32*) #0 {
  %3 = alloca i32, align 4
  %4 = alloca i32*, align 8
  %5 = alloca i32, align 4
  store i32 %0, i32* %3, align 4
  store i32* %1, i32** %4, align 8
  %6 = load i32, i32* %3, align 4
  %7 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str.2, i32 0, i32 0), i32 %6)
  store i32 0, i32* %5, align 4
  br label %8

; <label>:8:                                      ; preds = %19, %2
  %9 = load i32, i32* %5, align 4
  %10 = load i32, i32* %3, align 4
  %11 = icmp slt i32 %9, %10
  br i1 %11, label %12, label %22

; <label>:12:                                     ; preds = %8
  %13 = load i32*, i32** %4, align 8
  %14 = load i32, i32* %5, align 4
  %15 = sext i32 %14 to i64
  %16 = getelementptr inbounds i32, i32* %13, i64 %15
  %17 = load i32, i32* %16, align 4
  %18 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str.3, i32 0, i32 0), i32 %17)
  br label %19

; <label>:19:                                     ; preds = %12
  %20 = load i32, i32* %5, align 4
  %21 = add nsw i32 %20, 1
  store i32 %21, i32* %5, align 4
  br label %8

; <label>:22:                                     ; preds = %8
  %23 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([2 x i8], [2 x i8]* @.str.4, i32 0, i32 0))
  ret void
}

attributes #0 = { noinline nounwind optnone uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #1 = { "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }

!llvm.ident = !{!0}
!llvm.module.flags = !{!1}

!0 = !{!"clang version 6.0.1-14 (tags/RELEASE_601/final)"}
!1 = !{i32 1, !"wchar_size", i32 4}
