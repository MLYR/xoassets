import 'package:flutter/material.dart';

import '../design/xo_gradients.dart';
import '../design/xo_radius.dart';
import '../design/xo_shadows.dart';
import '../design/xo_text_styles.dart';

/// XO 主按钮组件，统一深青绿渐变、高度、圆角和加载态。
class XoButton extends StatelessWidget {
  const XoButton({
    required this.label,
    required this.onPressed,
    super.key,
    this.isLoading = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final bool isLoading;

  @override
  Widget build(BuildContext context) {
    final enabled = onPressed != null && !isLoading;
    return Opacity(
      opacity: enabled ? 1 : 0.62,
      child: DecoratedBox(
        decoration: BoxDecoration(
          gradient: XoGradients.primaryButton,
          borderRadius: BorderRadius.circular(XoRadius.button),
          boxShadow: const [XoShadows.button],
        ),
        child: Material(
          color: Colors.transparent,
          borderRadius: BorderRadius.circular(XoRadius.button),
          child: InkWell(
            onTap: enabled ? onPressed : null,
            borderRadius: BorderRadius.circular(XoRadius.button),
            child: SizedBox(
              height: 56,
              width: double.infinity,
              child: Center(
                child: isLoading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : Text(label, style: XoTextStyles.button),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
