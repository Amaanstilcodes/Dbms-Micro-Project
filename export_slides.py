import os
import win32com.client

def export_slides():
    ppt_path = os.path.abspath("ISP_Management_System_Presentation.pptx")
    out_dir = os.path.abspath("slide_images")
    if not os.path.exists(out_dir):
        os.makedirs(out_dir)

    powerpoint = win32com.client.Dispatch("PowerPoint.Application")
    # Open presentation without making it visibly focus stealing
    deck = powerpoint.Presentations.Open(ppt_path, ReadOnly=True, Untitled=False, WithWindow=False)

    for i, slide in enumerate(deck.Slides):
        img_path = os.path.join(out_dir, f"slide_{i+1}.png")
        # Export(Path, FilterName, ScaleWidth, ScaleHeight)
        slide.Export(img_path, "PNG", 1920, 1080)
        print(f"Exported: {img_path}")

    deck.Close()
    powerpoint.Quit()
    print("All slides exported successfully.")

if __name__ == "__main__":
    export_slides()
